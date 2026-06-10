package com.photolist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photolist.model.AnalyzeRequest;
import com.photolist.model.AnalyzeResponse;
import com.photolist.service.MercadoLibreService;
import com.photolist.service.ServiceException;
import com.photolist.service.VisionService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;

public class AnalyzePhotoHandler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final int CACHE_TTL_DAYS = 7;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String tableName = System.getenv("TABLE_NAME");
    private final VisionService visionService = new VisionService();
    private final MercadoLibreService mlService = new MercadoLibreService();
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            AnalyzeRequest request = parseRequest(event);
            if (request.getImageKey() == null || request.getImageKey().isBlank()) {
                return jsonResponse(400, Map.of("error", "imageKey is required"));
            }

            String imageKey = request.getImageKey().trim();
            String imageHash = sha256Hex(imageKey);

            AnalyzeResponse cached = loadFromCache(imageHash);
            if (cached != null) {
                cached.setCachedAt(cached.getAnalyzedAt());
                return jsonResponse(200, cached);
            }

            VisionService.IdentifyResult identified = visionService.identify(imageKey);
            if (identified == null) {
                return jsonResponse(422, Map.of("error", "Item could not be identified"));
            }

            String site = request.getSite();
            AnalyzeResponse.MarketInfo market = mlService.buildSearchLink(identified.getSearchQuery(), site);

            String analyzedAt = Instant.now().toString();
            AnalyzeResponse response = new AnalyzeResponse();
            response.setItem(identified.getItem());
            response.setMarket(market);
            response.setAnalyzedAt(analyzedAt);
            response.setCachedAt(null);

            saveToCache(imageHash, response);

            return jsonResponse(200, response);
        } catch (ServiceException e) {
            return jsonResponse(502, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return jsonResponse(400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error [" + e.getClass().getName() + "]: " + e.getMessage());
            e.printStackTrace(System.err);
            return jsonResponse(500, Map.of("error", "Internal server error"));
        }
    }

    private AnalyzeRequest parseRequest(APIGatewayProxyRequestEvent event) {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Request body is required");
        }
        try {
            return MAPPER.readValue(body, AnalyzeRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON body");
        }
    }

    private AnalyzeResponse loadFromCache(String imageHash) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("TABLE_NAME environment variable is not set");
        }

        var item = dynamoDb.getItem(GetItemRequest.builder()
                        .tableName(tableName)
                        .key(Map.of("imageHash", AttributeValue.builder().s(imageHash).build()))
                        .build())
                .item();

        if (item == null || item.isEmpty()) {
            return null;
        }

        AttributeValue ttlAttr = item.get("ttl");
        AttributeValue resultAttr = item.get("resultJson");
        if (ttlAttr == null || resultAttr == null || resultAttr.s() == null) {
            return null;
        }

        long ttlEpoch = Long.parseLong(ttlAttr.n());
        if (ttlEpoch <= Instant.now().getEpochSecond()) {
            return null;
        }

        try {
            return MAPPER.readValue(resultAttr.s(), AnalyzeResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize cached result", e);
        }
    }

    private void saveToCache(String imageHash, AnalyzeResponse response) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("TABLE_NAME environment variable is not set");
        }

        String resultJson;
        try {
            resultJson = MAPPER.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize analysis result", e);
        }

        long ttlEpoch = Instant.now().plus(CACHE_TTL_DAYS, ChronoUnit.DAYS).getEpochSecond();

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "imageHash", AttributeValue.builder().s(imageHash).build(),
                        "ttl", AttributeValue.builder().n(String.valueOf(ttlEpoch)).build(),
                        "resultJson", AttributeValue.builder().s(resultJson).build()))
                .build());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
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
