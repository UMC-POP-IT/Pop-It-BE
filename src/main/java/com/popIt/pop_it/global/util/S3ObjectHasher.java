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
        String host = uri.getHost();
        String path = uri.getPath();

        // URL 유효성 및 S3 도메인 여부 1차 검증
        if (host == null || !host.contains(".s3.") || path == null || path.length() <= 1) {
            throw new IllegalArgumentException("유효한 S3 객체 URL이 아닙니다.");
        }

        // 마침표가 포함된 버킷명도 정상적으로 추출되도록 처리
        String bucket = host.substring(0, host.indexOf(".s3."));
        String key = path.substring(1); // 앞의 '/' 제거

        // 전체 데이터를 메모리에 올리지 않고 스트림 방식으로 해시 계산
        try (java.io.InputStream is = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build(),
                software.amazon.awssdk.core.sync.ResponseTransformer.toInputStream())) {

            return HashUtil.sha256(is.toString());
        } catch (Exception e) {
            throw new IllegalStateException("서명 이미지 위변조 검증 중 해시 계산에 실패했습니다.", e);
        }
    }
}
