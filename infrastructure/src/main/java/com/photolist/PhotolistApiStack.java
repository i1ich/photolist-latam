package com.photolist;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.Map;

public class PhotolistApiStack extends Stack {

    public PhotolistApiStack(final Construct scope, final String id, final StackProps props,
                             final PhotolistStorageStack storageStack) {
        super(scope, id, props);

        // Lambda: generate-upload-url
        Function generateUploadUrlFn = Function.Builder.create(this, "GenerateUploadUrlFn")
                .functionName("photolist-generate-upload-url")
                .runtime(Runtime.JAVA_21)
                .handler("com.photolist.GenerateUploadUrlHandler::handleRequest")
                .code(Code.fromAsset("../functions/generate-upload-url/target/generate-upload-url.jar"))
                .memorySize(512)
                .timeout(Duration.seconds(10))
                .environment(Map.of(
                        "BUCKET_NAME", storageStack.getPhotoUploadsBucket().getBucketName()
                ))
                .build();

        storageStack.getPhotoUploadsBucket().grantPut(generateUploadUrlFn);

        // Lambda: analyze-photo
        Function analyzePhotoFn = Function.Builder.create(this, "AnalyzePhotoFn")
                .functionName("photolist-analyze-photo")
                .runtime(Runtime.JAVA_21)
                .handler("com.photolist.AnalyzePhotoHandler::handleRequest")
                .code(Code.fromAsset("../functions/analyze-photo/target/analyze-photo.jar"))
                .memorySize(1024)
                .timeout(Duration.seconds(30))
                .environment(Map.of(
                        "BUCKET_NAME", storageStack.getPhotoUploadsBucket().getBucketName(),
                        "TABLE_NAME", storageStack.getResultsCacheTable().getTableName()
                ))
                .build();

        storageStack.getPhotoUploadsBucket().grantRead(analyzePhotoFn);
        storageStack.getResultsCacheTable().grantReadWriteData(analyzePhotoFn);

        // API Gateway
        RestApi api = RestApi.Builder.create(this, "PhotolistApi")
                .restApiName("photolist-api")
                .description("PhotoList LATAM REST API")
                .build();

        Resource uploadResource = api.getRoot().addResource("upload-url");
        uploadResource.addMethod("POST", new LambdaIntegration(generateUploadUrlFn));

        Resource analyzeResource = api.getRoot().addResource("analyze");
        analyzeResource.addMethod("POST", new LambdaIntegration(analyzePhotoFn));
    }
}
