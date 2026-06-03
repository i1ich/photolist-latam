package com.photolist;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class PhotolistApp {

    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region("sa-east-1")
                .build();

        StackProps props = StackProps.builder()
                .env(env)
                .build();

        PhotolistStorageStack storageStack = new PhotolistStorageStack(app, "PhotolistStorageStack", props);
        new PhotolistApiStack(app, "PhotolistApiStack", props, storageStack);
        new PhotolistFrontendStack(app, "PhotolistFrontendStack", props);

        app.synth();
    }
}
