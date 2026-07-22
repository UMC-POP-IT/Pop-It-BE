package com.popIt.pop_it.global.util;

import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.io.InputStream;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

// S3에 저장된 파일의 위변조 검증용 유틸리티.
// URL 문자열만 해시하면 파일이 나중에 바뀌어도 감지할 수 없으므로, 서버가 객체 내용을 직접 읽어 해시한다.
@Component
@RequiredArgsConstructor
public class S3ObjectHasher {

    private final S3Client s3Client;

    // fileUrl 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
    // fileUrl은 클라이언트가 그대로 보내는 값이라 신뢰할 수 없다. 여기서 뽑아낸 버킷/키를 그대로
    // 쓰면 서버의 AWS 자격증명으로 임의 버킷/키를 읽어오는 confused deputy가 될 수 있으므로,
    // 호출부가 넘긴 업로드 API 소유 버킷(expectedBucket)·사용자별 key prefix(expectedKeyPrefix)에
    // 속하는 객체만 읽도록 강제한다.
    public String hash(String fileUrl, String expectedBucket, String expectedKeyPrefix) {
        URI uri = URI.create(fileUrl);
        String host = uri.getHost();
        String path = uri.getPath();

        // URL 유효성 및 S3 도메인 여부 1차 검증
        if (host == null || !host.contains(".s3.") || path == null || path.length() <= 1) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST, "유효한 S3 객체 URL이 아닙니다.");
        }

        // 마침표가 포함된 버킷명도 정상적으로 추출되도록 처리
        String bucket = host.substring(0, host.indexOf(".s3."));
        String key = path.substring(1); // 앞의 '/' 제거

        if (!bucket.equals(expectedBucket)) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST, "허용되지 않은 버킷입니다.");
        }
        if (!key.startsWith(expectedKeyPrefix)) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST, "허용되지 않은 파일 경로입니다.");
        }

        // 전체 데이터를 메모리에 올리지 않고 스트림 방식으로 해시 계산
        try (InputStream s3Stream = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toInputStream())) {
            return HashUtil.sha256(s3Stream); // 스트림 자체가 아니라 실제 바이트 내용을 해시해야 한다
        } catch (Exception e) {
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR,
                    "서명 이미지 위변조 검증 중 해시 계산에 실패했습니다.");
        }
    }
}
