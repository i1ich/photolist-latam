package com.photolist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.photolist.model.AnalyzeResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

/**
 * Calls OpenAI vision to identify an item from a photo stored in S3.
 * Model and max-tokens are read from SSM at cold-start so they can be changed without redeployment.
 */
public class VisionService {

    private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final double MIN_CONFIDENCE = 0.5;
    private static final String DEFAULT_MODEL      = "gpt-4.1-nano";
    private static final int    DEFAULT_MAX_TOKENS = 300;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Cached at cold-start; volatile ensures visibility across threads inside the same container.
    private static volatile String cachedApiKey;
    private static volatile String cachedModel;
    private static volatile int    cachedMaxTokens = -1;

    private final String    bucketName = System.getenv("BUCKET_NAME");
    private final S3Client  s3Client   = S3Client.create();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Identified item plus MercadoLibre-optimized search query.
     */
    public static class IdentifyResult {
        private final AnalyzeResponse.ItemInfo item;
        private final String searchQuery;

        public IdentifyResult(AnalyzeResponse.ItemInfo item, String searchQuery) {
            this.item = item;
            this.searchQuery = searchQuery;
        }

        public AnalyzeResponse.ItemInfo getItem() {
            return item;
        }

        public String getSearchQuery() {
            return searchQuery;
        }
    }

    /**
     * Fetch image from S3, identify via OpenAI vision, and return structured result.
     *
     * @param imageKey S3 object key for the uploaded photo
     * @return identification result, or {@code null} if confidence &lt; 0.5
     */
    public IdentifyResult identify(String imageKey) {
        byte[] imageBytes = fetchImageFromS3(imageKey);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = mimeTypeForKey(imageKey);
        String assistantContent = callOpenAi(base64Image, mimeType);
        return parseIdentifyResult(assistantContent);
    }

    private static String getApiKey() {
        if (cachedApiKey != null) return cachedApiKey;
        synchronized (VisionService.class) {
            if (cachedApiKey != null) return cachedApiKey;
            String paramName = System.getenv("OPENAI_API_KEY_PARAM");
            if (paramName == null || paramName.isBlank()) {
                throw new IllegalStateException("OPENAI_API_KEY_PARAM env var not set");
            }
            try (SsmClient ssm = SsmClient.create()) {
                cachedApiKey = ssm.getParameter(GetParameterRequest.builder()
                                .name(paramName).withDecryption(true).build())
                        .parameter().value();
            }
            if (cachedApiKey == null || cachedApiKey.isBlank()) {
                throw new IllegalStateException("OpenAI API key SSM param is empty: " + paramName);
            }
            return cachedApiKey;
        }
    }

    /** Reads model name from SSM; falls back to DEFAULT_MODEL if param is absent or blank. */
    private static String getModel() {
        if (cachedModel != null) return cachedModel;
        synchronized (VisionService.class) {
            if (cachedModel != null) return cachedModel;
            String paramName = System.getenv("VISION_MODEL_PARAM");
            cachedModel = readSsmString(paramName, DEFAULT_MODEL);
            return cachedModel;
        }
    }

    /** Reads max_tokens from SSM; falls back to DEFAULT_MAX_TOKENS if param is absent or blank. */
    private static int getMaxTokens() {
        if (cachedMaxTokens >= 0) return cachedMaxTokens;
        synchronized (VisionService.class) {
            if (cachedMaxTokens >= 0) return cachedMaxTokens;
            String paramName = System.getenv("VISION_MAX_TOKENS_PARAM");
            String raw = readSsmString(paramName, String.valueOf(DEFAULT_MAX_TOKENS));
            try {
                cachedMaxTokens = Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                cachedMaxTokens = DEFAULT_MAX_TOKENS;
            }
            return cachedMaxTokens;
        }
    }

    /** Helper: reads a plain-String SSM parameter; returns {@code fallback} on any error. */
    private static String readSsmString(String paramName, String fallback) {
        if (paramName == null || paramName.isBlank()) return fallback;
        try (SsmClient ssm = SsmClient.create()) {
            String value = ssm.getParameter(GetParameterRequest.builder()
                            .name(paramName).withDecryption(false).build())
                    .parameter().value();
            return (value == null || value.isBlank()) ? fallback : value;
        } catch (Exception e) {
            // Parameter may not exist yet during local testing — use default.
            return fallback;
        }
    }

    private byte[] fetchImageFromS3(String imageKey) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("BUCKET_NAME environment variable is not set");
        }
        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(imageKey)
                        .build())) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new ServiceException("Failed to read image from S3: " + imageKey + " — " + e.getMessage());
        }
    }

    private static String mimeTypeForKey(String imageKey) {
        String lower = imageKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    private String callOpenAi(String base64Image, String mimeType) {
        String requestBody;
        try {
            requestBody = buildRequestBody(base64Image, mimeType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build OpenAI request", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_CHAT_COMPLETIONS_URL))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("OpenAI API request failed: " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceException("OpenAI API request failed: " + e.getMessage());
        }

        if (response.statusCode() != 200) {
            throw new ServiceException(
                    "OpenAI API error: HTTP " + response.statusCode() + " — " + response.body());
        }

        try {
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new ServiceException("OpenAI response missing message content");
            }
            return content.asText();
        } catch (IOException e) {
            throw new ServiceException("Failed to parse OpenAI response: " + e.getMessage());
        }
    }

    private String buildRequestBody(String base64Image, String mimeType) throws IOException {
        String prompt = """
                Identifica el artículo en esta foto. Responde ÚNICAMENTE con JSON válido:
                {"name": "...", "brand": "...", "category": "...", "confidence": 0.0-1.0, "searchQuery": "..."}
                searchQuery debe estar optimizado para búsqueda en MercadoLibre (conciso, marca + modelo).
                """;

        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        ObjectNode imageUrl = MAPPER.createObjectNode();
        imageUrl.put("url", dataUrl);

        ObjectNode imagePart = MAPPER.createObjectNode();
        imagePart.put("type", "image_url");
        imagePart.set("image_url", imageUrl);

        ObjectNode textPart = MAPPER.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", prompt.trim());

        ArrayNode content = MAPPER.createArrayNode();
        content.add(textPart);
        content.add(imagePart);

        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        message.set("content", content);

        ArrayNode messages = MAPPER.createArrayNode();
        messages.add(message);

        ObjectNode responseFormat = MAPPER.createObjectNode();
        responseFormat.put("type", "json_object");

        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", getModel());
        body.put("max_tokens", getMaxTokens());
        body.set("response_format", responseFormat);
        body.set("messages", messages);

        return MAPPER.writeValueAsString(body);
    }

    private IdentifyResult parseIdentifyResult(String assistantContent) {
        String json = extractJsonPayload(assistantContent);
        try {
            JsonNode node = MAPPER.readTree(json);
            double confidence = node.path("confidence").asDouble(0.0);
            if (confidence < MIN_CONFIDENCE) {
                return null;
            }

            String name = textOrNull(node, "name");
            String brand = textOrNull(node, "brand");
            String category = textOrNull(node, "category");
            String searchQuery = textOrNull(node, "searchQuery");
            if (searchQuery == null || searchQuery.isBlank()) {
                searchQuery = name;
            }

            AnalyzeResponse.ItemInfo item = new AnalyzeResponse.ItemInfo();
            item.setName(name);
            item.setBrand(brand);
            item.setCategory(category);
            item.setConfidence(confidence);

            return new IdentifyResult(item, searchQuery);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse vision model JSON: " + assistantContent, e);
        }
    }

    private static String extractJsonPayload(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int closingFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && closingFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, closingFence).trim();
            }
        }
        return trimmed;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}
