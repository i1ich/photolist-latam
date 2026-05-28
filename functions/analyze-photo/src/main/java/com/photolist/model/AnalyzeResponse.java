package com.photolist.model;

import java.util.List;

public class AnalyzeResponse {

    private String itemName;
    private String category;
    private String site;
    private PriceRange priceRange;
    private List<Listing> topListings;

    public static class PriceRange {
        private double min;
        private double max;
        private double median;
        private String currency;

        public double getMin() { return min; }
        public void setMin(double min) { this.min = min; }
        public double getMax() { return max; }
        public void setMax(double max) { this.max = max; }
        public double getMedian() { return median; }
        public void setMedian(double median) { this.median = median; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

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

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public PriceRange getPriceRange() { return priceRange; }
    public void setPriceRange(PriceRange priceRange) { this.priceRange = priceRange; }
    public List<Listing> getTopListings() { return topListings; }
    public void setTopListings(List<Listing> topListings) { this.topListings = topListings; }
}
