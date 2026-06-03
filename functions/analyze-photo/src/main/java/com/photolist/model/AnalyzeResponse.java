package com.photolist.model;

import java.util.List;

public class AnalyzeResponse {

    private ItemInfo item;
    private MarketInfo market;
    private String cachedAt;
    private String analyzedAt;

    public static class ItemInfo {
        private String name;
        private String category;
        private String brand;
        private double confidence;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }

    public static class MarketInfo {
        private String site;
        private String currency;
        private double priceMin;
        private double priceMax;
        private double priceMedian;
        private List<Listing> topListings;

        public static class Listing {
            private String title;
            private double price;
            private String currency;
            private String condition;
            private String url;
            private String thumbnail;

            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
            public double getPrice() { return price; }
            public void setPrice(double price) { this.price = price; }
            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
            public String getCondition() { return condition; }
            public void setCondition(String condition) { this.condition = condition; }
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public String getThumbnail() { return thumbnail; }
            public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
        }

        public String getSite() { return site; }
        public void setSite(String site) { this.site = site; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public double getPriceMin() { return priceMin; }
        public void setPriceMin(double priceMin) { this.priceMin = priceMin; }
        public double getPriceMax() { return priceMax; }
        public void setPriceMax(double priceMax) { this.priceMax = priceMax; }
        public double getPriceMedian() { return priceMedian; }
        public void setPriceMedian(double priceMedian) { this.priceMedian = priceMedian; }
        public List<Listing> getTopListings() { return topListings; }
        public void setTopListings(List<Listing> topListings) { this.topListings = topListings; }
    }

    public ItemInfo getItem() { return item; }
    public void setItem(ItemInfo item) { this.item = item; }
    public MarketInfo getMarket() { return market; }
    public void setMarket(MarketInfo market) { this.market = market; }
    public String getCachedAt() { return cachedAt; }
    public void setCachedAt(String cachedAt) { this.cachedAt = cachedAt; }
    public String getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(String analyzedAt) { this.analyzedAt = analyzedAt; }
}
