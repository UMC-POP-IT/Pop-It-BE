package com.popIt.pop_it.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws")
public record AwsProperties(
        Credentials credentials,
        String region,
        S3 s3
) {
    public record Credentials(String accessKey, String secretKey) {}

    // bucket: 일반(공간 이미지 등), hostDocumentBucket: 민감서류(통장/사업자등록증) 전용 프라이빗 버킷
    public record S3(String bucket, String hostDocumentBucket) {}
}