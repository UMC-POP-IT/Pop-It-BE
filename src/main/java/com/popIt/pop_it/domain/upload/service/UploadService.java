package com.popIt.pop_it.domain.upload.service;

import com.popIt.pop_it.domain.upload.dto.UploadReqDTO;
import com.popIt.pop_it.domain.upload.dto.UploadResDTO;
import com.popIt.pop_it.domain.upload.enums.UploadType;
import com.popIt.pop_it.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    private static final Duration EXPIRATION = Duration.ofMinutes(10);

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "application/pdf", "pdf"
    );

    public UploadResDTO.PresignedUrlListRes issuePresignedUrls(Long userId, UploadReqDTO.PresignedUrlReq request) {

        List<UploadResDTO.PresignedUrlInfoRes> uploads = request.files().stream()
                .map(file -> issueOne(userId, request.uploadType(), file.contentType()))
                .toList();

        return new UploadResDTO.PresignedUrlListRes(uploads);
    }

    private UploadResDTO.PresignedUrlInfoRes issueOne(Long userId, UploadType uploadType, String contentType) {
        String extension = ALLOWED_CONTENT_TYPES.get(contentType);

        // 민감서류(HOST_DOCUMENT)는 프라이빗 전용 버킷으로, 일반 이미지는 기본 버킷으로 분기
        String bucket = resolveBucket(uploadType);
        // S3 key: {uploadType}/{userId}/{uuid}.{ext} — 유저별 경로로 분리해 관리
        String key = uploadType.getPath() + "/" + userId + "/" + UUID.randomUUID() + "." + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignedRequest = PutObjectPresignRequest.builder()
                .signatureDuration(EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();

        // presignedUrl: 프론트가 S3에 직접 PUT 업로드할 때 사용 (10분 유효)
        // fileUrl: 업로드 완료 후 DB에 저장할 영구 접근 URL
        String presignedUrl = s3Presigner.presignPutObject(presignedRequest).url().toString();
        String fileUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, awsProperties.region(), key);

        return new UploadResDTO.PresignedUrlInfoRes(presignedUrl, fileUrl);
    }

    // UploadType.BucketType에 따라 실제 버킷 이름 반환 (새 버킷 추가 시 여기에만 추가)
    private String resolveBucket(UploadType uploadType) {
        return switch (uploadType.getBucketType()) {
            case GENERAL -> awsProperties.s3().bucket();
            case HOST_DOCUMENT -> awsProperties.s3().hostDocumentBucket();
        };
    }
}
