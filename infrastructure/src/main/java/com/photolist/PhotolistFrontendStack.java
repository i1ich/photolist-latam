package com.photolist;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.util.List;

/**
 * Static hosting for the PWA: a private S3 bucket served through CloudFront (HTTPS, CDN).
 * Bucket contents are uploaded out-of-band ({@code aws s3 sync}) so app deploys stay fast
 * and decoupled from infrastructure changes — see the deploy guide.
 */
public class PhotolistFrontendStack extends Stack {

    public PhotolistFrontendStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Bucket siteBucket = Bucket.Builder.create(this, "FrontendBucket")
                .versioned(false)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        Distribution distribution = Distribution.Builder.create(this, "FrontendDistribution")
                .comment("PhotoList LATAM PWA")
                .defaultRootObject("index.html")
                .defaultBehavior(BehaviorOptions.builder()
                        .origin(new S3Origin(siteBucket))
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .build())
                // SPA fallback: unknown paths return index.html so client-side routing works.
                .errorResponses(List.of(
                        ErrorResponse.builder()
                                .httpStatus(403)
                                .responseHttpStatus(200)
                                .responsePagePath("/index.html")
                                .ttl(Duration.minutes(5))
                                .build(),
                        ErrorResponse.builder()
                                .httpStatus(404)
                                .responseHttpStatus(200)
                                .responsePagePath("/index.html")
                                .ttl(Duration.minutes(5))
                                .build()
                ))
                .build();

        CfnOutput.Builder.create(this, "FrontendBucketName")
                .description("Run: aws s3 sync frontend/dist s3://<this>/ --delete")
                .value(siteBucket.getBucketName())
                .build();

        CfnOutput.Builder.create(this, "FrontendUrl")
                .description("Public URL of the PWA")
                .value("https://" + distribution.getDistributionDomainName())
                .build();

        CfnOutput.Builder.create(this, "FrontendDistributionId")
                .description("CloudFront distribution id — use to invalidate the cache after a sync")
                .value(distribution.getDistributionId())
                .build();
    }
}
