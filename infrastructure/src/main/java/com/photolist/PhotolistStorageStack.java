package com.photolist;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.s3.*;
import software.constructs.Construct;

import java.util.List;

public class PhotolistStorageStack extends Stack {

    private final Bucket photoUploadsBucket;
    private final Table resultsCacheTable;

    public PhotolistStorageStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // S3 bucket for photo uploads — 24h lifecycle
        photoUploadsBucket = Bucket.Builder.create(this, "PhotoUploadsBucket")
                .versioned(false)
                .lifecycleRules(List.of(
                        LifecycleRule.builder()
                                .expiration(Duration.days(1))
                                .build()
                ))
                .cors(List.of(
                        CorsRule.builder()
                                .allowedMethods(List.of(HttpMethods.PUT, HttpMethods.POST))
                                .allowedOrigins(List.of("*"))
                                .allowedHeaders(List.of("*"))
                                .build()
                ))
                .build();

        // DynamoDB table for results cache
        resultsCacheTable = Table.Builder.create(this, "ResultsCacheTable")
                .tableName("photolist-results-cache")
                .partitionKey(Attribute.builder()
                        .name("imageHash")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl")
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();
    }

    public Bucket getPhotoUploadsBucket() {
        return photoUploadsBucket;
    }

    public Table getResultsCacheTable() {
        return resultsCacheTable;
    }
}
