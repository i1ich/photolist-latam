package com.photolist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.photolist.model.AnalyzeRequest;
import com.photolist.model.AnalyzeResponse;
import com.photolist.service.VisionService;
import com.photolist.service.MercadoLibreService;

public class AnalyzePhotoHandler implements RequestHandler<AnalyzeRequest, AnalyzeResponse> {

    private final VisionService visionService = new VisionService();
    private final MercadoLibreService mlService = new MercadoLibreService();

    @Override
    public AnalyzeResponse handleRequest(AnalyzeRequest request, Context context) {
        // 1. Get image from S3
        // 2. Call Vision LLM → item identification
        // 3. Call MercadoLibre Search API → prices
        // 4. Cache result in DynamoDB
        // 5. Return response
        throw new UnsupportedOperationException("TODO: implement");
    }
}
