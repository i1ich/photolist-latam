package com.photolist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class GenerateUploadUrlHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String bucketName = System.getenv("BUCKET_NAME");
    private final S3Presigner presigner = S3Presigner.create();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        String imageKey = "uploads/" + UUID.randomUUID() + ".jpg";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(imageKey)
                .contentType("image/jpeg")
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest));

        return Map.of(
                "uploadUrl", presignedRequest.url().toString(),
                "imageKey", imageKey,
                "expiresIn", 300
        );
    }
}
