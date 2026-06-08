package com.photolist;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class PhotolistApiStack extends Stack {

    /**
     * Name of the SSM parameter holding the OpenAI API key. It is created OUTSIDE CloudFormation
     * (see the deploy guide) as a {@code SecureString}, so CDK never sees or overwrites the secret.
     */
    private static final String OPENAI_API_KEY_PARAM = "/photolist/openai-api-key";

    /**
     * SSM parameters for model configuration (plain String, not secret).
     * Created outside CDK so values can be changed without redeployment.
     */
    private static final String VISION_MODEL_PARAM      = "/photolist/vision-model";
    private static final String VISION_MAX_TOKENS_PARAM = "/photolist/vision-max-tokens";

    // MercadoLibre OAuth credentials — all created outside CDK
    private static final String ML_CLIENT_ID_PARAM     = "/photolist/ml/client_id";
    private static final String ML_CLIENT_SECRET_PARAM = "/photolist/ml/client_secret";
    private static final String ML_REFRESH_TOKEN_PARAM = "/photolist/ml/refresh_token";

    public PhotolistApiStack(final Construct scope, final String id, final StackProps props,
                             final PhotolistStorageStack storageStack) {
        super(scope, id, props);

        // Lambda: generate-upload-url
        Function generateUploadUrlFn = Function.Builder.create(this, "GenerateUploadUrlFn")
                .functionName("photolist-generate-upload-url")
                .runtime(Runtime.JAVA_21)
                .handler("com.photolist.GenerateUploadUrlHandler::handleRequest")
                .code(Code.fromAsset("../functions/generate-upload-url/target/generate-upload-url.jar"))
                .memorySize(256)
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
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .environment(Map.of(
                        "BUCKET_NAME",            storageStack.getPhotoUploadsBucket().getBucketName(),
                        "TABLE_NAME",             storageStack.getResultsCacheTable().getTableName(),
                        // Secrets (SecureString) — fetched at runtime via SSM SDK, never in plain env vars
                        "OPENAI_API_KEY_PARAM",   OPENAI_API_KEY_PARAM,
                        // Model config (String) — fetched at runtime so values are changeable without redeploy
                        "VISION_MODEL_PARAM",     VISION_MODEL_PARAM,
                        "VISION_MAX_TOKENS_PARAM", VISION_MAX_TOKENS_PARAM,
                        // MercadoLibre OAuth — SSM param names only, secrets fetched at runtime
                        "ML_SITE",               "MLU"
                ))
                .build();

        storageStack.getPhotoUploadsBucket().grantRead(analyzePhotoFn);
        storageStack.getResultsCacheTable().grantReadWriteData(analyzePhotoFn);

        // Allow the analyzer to read the externally-managed SecureString OpenAI key and decrypt it,
        // plus the plain-String model config parameters.
        String ssmBase = "arn:aws:ssm:" + getRegion() + ":" + getAccount() + ":parameter";
        analyzePhotoFn.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("ssm:GetParameter"))
                .resources(List.of(
                        ssmBase + OPENAI_API_KEY_PARAM,
                        ssmBase + VISION_MODEL_PARAM,
                        ssmBase + VISION_MAX_TOKENS_PARAM,
                        ssmBase + ML_CLIENT_ID_PARAM,
                        ssmBase + ML_CLIENT_SECRET_PARAM,
                        ssmBase + ML_REFRESH_TOKEN_PARAM
                ))
                .build());
        // refresh_token rotates on every ML API call — Lambda must write the new one back
        analyzePhotoFn.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("ssm:PutParameter"))
                .resources(List.of(ssmBase + ML_REFRESH_TOKEN_PARAM))
                .build());
        analyzePhotoFn.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("kms:Decrypt", "kms:GenerateDataKey"))
                .resources(List.of("arn:aws:kms:" + getRegion() + ":" + getAccount() + ":alias/aws/ssm"))
                .build());

        // API Gateway with CORS preflight for the browser PWA.
        RestApi api = RestApi.Builder.create(this, "PhotolistApi")
                .restApiName("photolist-api")
                .description("PhotoList LATAM REST API")
                .defaultCorsPreflightOptions(CorsOptions.builder()
                        .allowOrigins(Cors.ALL_ORIGINS)
                        .allowMethods(List.of("POST", "OPTIONS"))
                        .allowHeaders(List.of("Content-Type"))
                        .build())
                .build();

        Resource uploadResource = api.getRoot().addResource("upload-url");
        uploadResource.addMethod("POST", new LambdaIntegration(generateUploadUrlFn));

        Resource analyzeResource = api.getRoot().addResource("analyze");
        analyzeResource.addMethod("POST", new LambdaIntegration(analyzePhotoFn));
    }
}
