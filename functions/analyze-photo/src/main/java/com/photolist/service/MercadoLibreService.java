package com.photolist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.photolist.model.AnalyzeResponse;

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
 * Starts with MLA (Argentina), expandable to MLB, MLM, MLU.
 */
public class MercadoLibreService {

    private static final String BASE_URL = "https://api.mercadolibre.com";
    private static final int SEARCH_LIMIT = 10;
    private static final int TOP_LISTINGS = 3;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String defaultSite = System.getenv().getOrDefault("ML_SITE", "MLA");

    /**
     * Search MercadoLibre for listings matching the query.
     *
     * @param searchQuery search terms (e.g. from VisionService)
     * @param site        MercadoLibre site id (MLA, MLB, MLM, MLU); uses {@code ML_SITE} env when blank
     * @return market data with price stats and top listings
     */
    public AnalyzeResponse.MarketInfo search(String searchQuery, String site) {
        String resolvedSite = resolveSite(site);
        String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
        String url = BASE_URL + "/sites/" + resolvedSite + "/search?q=" + encodedQuery + "&limit=" + SEARCH_LIMIT;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
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

    private String resolveSite(String site) {
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
                listing.setUrl(item.path("permalink").asText(""));
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

    private static double computeMedian(List<Double> prices) {
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
