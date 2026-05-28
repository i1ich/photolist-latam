package com.photolist.service;

import com.photolist.model.AnalyzeResponse;

import java.util.List;

/**
 * Queries MercadoLibre Search API for listings matching the identified item.
 * Starts with MLA (Argentina), expandable to MLB, MLM, MLU.
 */
public class MercadoLibreService {

    private static final String BASE_URL = "https://api.mercadolibre.com";
    private final String site = System.getenv().getOrDefault("ML_SITE", "MLA");

    /**
     * Search for listings matching item name.
     *
     * @param itemName identified item name from VisionService
     * @return list of top listings with price info
     */
    public List<AnalyzeResponse.Listing> searchListings(String itemName) {
        // TODO: implement GET /sites/{site}/search?q={itemName}
        throw new UnsupportedOperationException("TODO: implement");
    }
}
