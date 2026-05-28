package com.photolist.service;

/**
 * Calls Vision LLM (GPT-4o mini) to identify item from image bytes.
 * Returns item name and category.
 */
public class VisionService {

    private final String apiKey = System.getenv("OPENAI_API_KEY");
    private final String model = System.getenv().getOrDefault("VISION_MODEL", "gpt-4o-mini");

    /**
     * Identify item from base64-encoded image.
     *
     * @param base64Image base64-encoded JPEG image
     * @return identified item name (e.g. "iPhone 13 128GB")
     */
    public String identifyItem(String base64Image) {
        // TODO: implement OpenAI Vision API call
        throw new UnsupportedOperationException("TODO: implement");
    }
}
