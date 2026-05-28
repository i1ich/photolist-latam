package com.photolist;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.ssm.StringParameter;
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
                .memorySize(128)
                .timeout(Duration.seconds(5))
                .environment(Map.of(
                        "BUCKET_NAME", storageStack.getPhotoUploadsBucket().getBucketName()
                ))
                .build();

        storageStack.getPhotoUploadsBucket().grantPut(generateUploadUrlFn);

        StringParameter openAiApiKeyParam = StringParameter.Builder.create(this, "OpenAiApiKeyParam")
                .parameterName("/photolist/openai-api-key")
                .stringValue("REPLACE_ME")
                .description("OpenAI API key — set manually post-deploy")
                .build();

        // Lambda: analyze-photo
        Function analyzePhotoFn = Function.Builder.create(this, "AnalyzePhotoFn")
                .functionName("photolist-analyze-photo")
                .runtime(Runtime.JAVA_21)
                .handler("com.photolist.AnalyzePhotoHandler::handleRequest")
                .code(Code.fromAsset("../functions/analyze-photo/target/analyze-photo.jar"))
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .environment(Map.of(
                        "BUCKET_NAME", storageStack.getPhotoUploadsBucket().getBucketName(),
                        "TABLE_NAME", storageStack.getResultsCacheTable().getTableName(),
                        "OPENAI_API_KEY_PARAM", openAiApiKeyParam.getParameterName()
                ))
                .build();

        storageStack.getPhotoUploadsBucket().grantRead(analyzePhotoFn);
        storageStack.getResultsCacheTable().grantReadWriteData(analyzePhotoFn);
        openAiApiKeyParam.grantRead(analyzePhotoFn);

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
