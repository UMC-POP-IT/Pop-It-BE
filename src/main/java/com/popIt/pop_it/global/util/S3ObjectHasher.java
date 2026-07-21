package com.popIt.pop_it.global.util;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

// S3에 저장된 파일의 위변조 검증용 유틸리티.
// URL 문자열만 해시하면 파일이 나중에 바뀌어도 감지할 수 없으므로, 서버가 객체 내용을 직접 읽어 해시한다.
@Component
@RequiredArgsConstructor
public class S3ObjectHasher {

    private final S3Client s3Client;

    // fileUrl 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
    public String hash(String fileUrl) {
        URI uri = URI.create(fileUrl);
        String bucket = uri.getHost().split("\\.")[0];
        String key = uri.getPath().substring(1); // 앞의 '/' 제거

        byte[] bytes = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build()
        ).asByteArray();

        return HashUtil.sha256(bytes);
    }
}
