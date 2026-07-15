package com.popIt.pop_it.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws")
public record AwsProperties(
        Credentials credentials,
        String region,
        S3 s3
) {
    public record Credentials(String accessKey, String secretKey) {}
    public record S3(String bucket) {}
}