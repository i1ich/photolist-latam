package com.photolist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photolist.model.AnalyzeResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterType;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Queries MercadoLibre Search API for listings matching the identified item.
 * Authentication uses OAuth 2.0 refresh_token flow:
 *   1. Read refresh_token from SSM (/photolist/ml/refresh_token)
 *   2. Exchange for access_token via /oauth/token
 *   3. Write new refresh_token back to SSM (ML tokens rotate on every use)
 *   4. Add Authorization: Bearer header to search request
 */
public class MercadoLibreService {

    private static final String BASE_URL          = "https://api.mercadolibre.com";
    private static final String TOKEN_URL         = "https://api.mercadolibre.com/oauth/token";
    private static final int    SEARCH_LIMIT      = 10;
    private static final int    TOP_LISTINGS      = 3;

    // SSM parameter names — must match setup-ssm-params.ps1
    private static final String PARAM_CLIENT_ID      = "/photolist/ml/client_id";
    private static final String PARAM_CLIENT_SECRET  = "/photolist/ml/client_secret";
    private static final String PARAM_REFRESH_TOKEN  = "/photolist/ml/refresh_token";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String defaultSite = System.getenv().getOrDefault("ML_SITE", "MLA");

    // ── Token refresh ────────────────────────────────────────────────────────────

    /**
     * Fetches a fresh access_token using the stored refresh_token, then rotates
     * the refresh_token in SSM (ML invalidates the old one immediately).
     */
    private String fetchAccessToken() {
        String clientId, clientSecret, refreshToken;
        try (SsmClient ssm = SsmClient.create()) {
            clientId = ssm.getParameter(GetParameterRequest.builder()
                    .name(PARAM_CLIENT_ID).withDecryption(false).build())
                    .parameter().value();
            clientSecret = ssm.getParameter(GetParameterRequest.builder()
                    .name(PARAM_CLIENT_SECRET).withDecryption(true).build())
                    .parameter().value();
            refreshToken = ssm.getParameter(GetParameterRequest.builder()
                    .name(PARAM_REFRESH_TOKEN).withDecryption(true).build())
                    .parameter().value();
        }

        String body = "grant_type=refresh_token"
                + "&client_id="     + URLEncoder.encode(clientId,     StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp;
        try {
            resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("ML token refresh interrupted", e);
        } catch (IOException e) {
            throw new ServiceException("ML token refresh failed", e);
        }

        if (resp.statusCode() != 200) {
            throw new ServiceException("ML token refresh HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(resp.body());
        } catch (IOException e) {
            throw new ServiceException("Failed to parse ML token response", e);
        }

        String newRefreshToken = json.path("refresh_token").asText(null);
        String accessToken     = json.path("access_token").asText(null);

        if (accessToken == null || accessToken.isBlank()) {
            throw new ServiceException("ML token response missing access_token");
        }

        // Rotate refresh_token in SSM (ML single-use tokens — old one is already invalid)
        if (newRefreshToken != null && !newRefreshToken.isBlank()) {
            try (SsmClient ssm = SsmClient.create()) {
                ssm.putParameter(PutParameterRequest.builder()
                        .name(PARAM_REFRESH_TOKEN)
                        .value(newRefreshToken)
                        .type(ParameterType.SECURE_STRING)
                        .overwrite(true)
                        .build());
            }
        }

        return accessToken;
    }

    // ── Search ───────────────────────────────────────────────────────────────────

    /**
     * Search MercadoLibre for listings matching the query.
     *
     * @param searchQuery search terms (e.g. from VisionService)
     * @param site        MercadoLibre site id (MLA, MLB, MLM, MLU); uses {@code ML_SITE} env when blank
     * @return market data with price stats and top listings
     */
    public AnalyzeResponse.MarketInfo search(String searchQuery, String site) {
        String resolvedSite  = resolveSite(site);
        String accessToken   = fetchAccessToken();
        String encodedQuery  = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
        String url           = BASE_URL + "/sites/" + resolvedSite + "/search?q=" + encodedQuery + "&limit=" + SEARCH_LIMIT;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("MercadoLibre request failed", e);
        } catch (IOException e) {
            throw new ServiceException("MercadoLibre request failed", e);
        }

        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new ServiceException("MercadoLibre auth error: HTTP " + status + " — check SSM credentials");
        }
        if (status == 429) {
            throw new ServiceException("MercadoLibre rate limited (429); retry after a few seconds");
        }
        if (status >= 500) {
            throw new ServiceException("MercadoLibre API error: HTTP " + status);
        }
        if (status < 200 || status >= 300) {
            throw new ServiceException("MercadoLibre API error: HTTP " + status);
        }

        return parseMarketInfo(response.body(), resolvedSite);
    }

    String resolveSite(String site) {
        if (site != null && !site.isBlank()) {
            return site.trim().toUpperCase();
        }
        return defaultSite;
    }

    private AnalyzeResponse.MarketInfo parseMarketInfo(String body, String site) {
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (IOException e) {
            throw new ServiceException("Failed to parse MercadoLibre response", e);
        }

        JsonNode results = root.path("results");
        List<Double> prices = new ArrayList<>();
        List<AnalyzeResponse.MarketInfo.Listing> allListings = new ArrayList<>();
        String currency = null;

        if (results.isArray()) {
            for (JsonNode item : results) {
                if (!item.has("price") || item.get("price").isNull()) {
                    continue;
                }
                double price = item.get("price").asDouble();
                prices.add(price);

                if (currency == null && item.hasNonNull("currency_id")) {
                    currency = item.get("currency_id").asText();
                }

                AnalyzeResponse.MarketInfo.Listing listing = new AnalyzeResponse.MarketInfo.Listing();
                listing.setTitle(item.path("title").asText(""));
                listing.setPrice(price);
                listing.setCurrency(item.path("currency_id").asText(""));
                listing.setCondition(item.path("condition").asText(""));
                listing.setUrl(item.path("permalink").asText(""));
                listing.setThumbnail(item.path("thumbnail").asText(""));
                allListings.add(listing);
            }
        }

        AnalyzeResponse.MarketInfo market = new AnalyzeResponse.MarketInfo();
        market.setSite(site);
        market.setCurrency(currency != null ? currency : "");
        market.setPriceMin(prices.isEmpty() ? 0 : Collections.min(prices));
        market.setPriceMax(prices.isEmpty() ? 0 : Collections.max(prices));
        market.setPriceMedian(computeMedian(prices));
        market.setTopListings(allListings.subList(0, Math.min(TOP_LISTINGS, allListings.size())));
        return market;
    }

    static double computeMedian(List<Double> prices) {
        if (prices.isEmpty()) {
            return 0;
        }
        List<Double> sorted = new ArrayList<>(prices);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
