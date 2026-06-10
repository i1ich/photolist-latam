package com.photolist.service;

import com.photolist.model.AnalyzeResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds a MercadoLibre deep-link URL for the identified item.
 * The /sites/{site}/search endpoint requires app certification (403 for uncertified apps),
 * so we return a search URL that the user opens directly in their browser.
 */
public class MercadoLibreService {

    private static final Map<String, String> SITE_DOMAINS = Map.of(
            "MLU", "mercadolibre.com.uy",
            "MLA", "mercadolibre.com.ar",
            "MLB", "mercadolibre.com.br",
            "MLM", "mercadolibre.com.mx",
            "MLC", "mercadolibre.cl",
            "MCO", "mercadolibre.com.co",
            "MLV", "mercadolibre.com.ve",
            "MPE", "mercadolibre.com.pe"
    );

    private final String defaultSite = System.getenv().getOrDefault("ML_SITE", "MLU");

    public AnalyzeResponse.MarketInfo buildSearchLink(String searchQuery, String site) {
        String resolvedSite = resolveSite(site);
        String domain = SITE_DOMAINS.getOrDefault(resolvedSite, "mercadolibre.com");
        String slug = searchQuery.trim()
                .toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .trim()
                .replaceAll("\\s+", "-");
        String searchUrl = "https://listado." + domain + "/" + slug;

        AnalyzeResponse.MarketInfo market = new AnalyzeResponse.MarketInfo();
        market.setSite(resolvedSite);
        market.setCurrency("");
        market.setPriceMin(0);
        market.setPriceMax(0);
        market.setPriceMedian(0);
        market.setTopListings(Collections.emptyList());
        market.setSearchUrl(searchUrl);
        return market;
    }

    String resolveSite(String site) {
        if (site != null && !site.isBlank()) {
            return site.trim().toUpperCase();
        }
        return defaultSite;
    }

    static double computeMedian(List<Double> prices) {
        if (prices.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(prices);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
