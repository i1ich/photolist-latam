package com.photolist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Returns a pre-signed S3 PUT URL so the client can upload a photo directly to S3.
 * Wired behind API Gateway proxy integration, so it consumes/produces the proxy
 * request/response envelope (status code, headers, JSON body).
 */
public class GenerateUploadUrlHandler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final int URL_TTL_SECONDS = 300;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String bucketName = System.getenv("BUCKET_NAME");
    private final S3Presigner presigner = S3Presigner.create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (bucketName == null || bucketName.isBlank()) {
            return jsonResponse(500, Map.of("error", "BUCKET_NAME environment variable is not set"));
        }

        String contentType = parseContentType(event);
        if (contentType == null) {
            return jsonResponse(400, Map.of("error",
                    "Unsupported contentType. Allowed: image/jpeg, image/png, image/webp"));
        }

        String imageKey = "uploads/" + UUID.randomUUID() + extensionFor(contentType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(imageKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofSeconds(URL_TTL_SECONDS))
                .putObjectRequest(putObjectRequest));

        return jsonResponse(200, Map.of(
                "uploadUrl", presignedRequest.url().toString(),
                "imageKey", imageKey,
                "expiresIn", URL_TTL_SECONDS));
    }

    /**
     * Read {@code contentType} from the JSON body and validate it. Defaults to
     * {@code image/jpeg} when the body is empty. Returns {@code null} when unsupported,
     * so the caller can reject with 400.
     */
    private String parseContentType(APIGatewayProxyRequestEvent event) {
        String body = event != null ? event.getBody() : null;
        String contentType = "image/jpeg";
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = MAPPER.readTree(body);
                JsonNode ct = node.get("contentType");
                if (ct != null && !ct.isNull() && !ct.asText().isBlank()) {
                    contentType = ct.asText().trim().toLowerCase();
                }
            } catch (Exception e) {
                return null;
            }
        }
        return switch (contentType) {
            case "image/jpeg", "image/png", "image/webp" -> contentType;
            default -> null;
        };
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private APIGatewayProxyResponseEvent jsonResponse(int statusCode, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*"))
                    .withBody(MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize response", e);
        }
    }
}
