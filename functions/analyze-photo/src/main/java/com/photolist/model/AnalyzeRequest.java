package com.photolist.model;

public class AnalyzeRequest {

    private String imageKey;  // S3 object key
    private String site = "MLA";  // MercadoLibre site: MLA, MLB, MLM, MLU

    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
}
