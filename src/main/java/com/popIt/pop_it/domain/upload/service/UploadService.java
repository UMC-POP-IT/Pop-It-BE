package com.popIt.pop_it.domain.upload.service;

import com.popIt.pop_it.domain.upload.dto.UploadReqDTO;
import com.popIt.pop_it.domain.upload.dto.UploadResDTO;
import com.popIt.pop_it.domain.upload.enums.UploadType;
import com.popIt.pop_it.domain.upload.exception.code.UploadErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
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

    public UploadResDTO.PresignedUrlList issuePresignedUrls(Long userId, UploadReqDTO.PresignedUrl request) {

        List<UploadResDTO.PresignedUrlInfo> uploads = request.files().stream()
                .map(file -> issueOne(userId, request.uploadType(), file.contentType()))
                .toList();

        return new UploadResDTO.PresignedUrlList(uploads);
    }

    private UploadResDTO.PresignedUrlInfo issueOne(Long userId, UploadType uploadType, String contentType) {
        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new ProjectException(UploadErrorCode.PRESIGNED_URL_UNSUPPORTED_CONTENT_TYPE);
        }

        // 업로드 타입에 따라 대상 버킷 분기 (민감서류는 프라이빗 버킷으로 격리)
        String bucket = resolveBucket(uploadType);
        // 유저(호스트)별로 파일을 분리 관리: {path}/{userId}/{uuid}.{ext}
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

        String presignedUrl = s3Presigner.presignPutObject(presignedRequest).url().toString();
        String fileUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, awsProperties.region(), key);

        return new UploadResDTO.PresignedUrlInfo(presignedUrl, fileUrl);
    }

    // 업로드 타입의 버킷 구분에 따라 실제 버킷 이름을 선택
    private String resolveBucket(UploadType uploadType) {
        return switch (uploadType.getBucketType()) {
            case GENERAL -> awsProperties.s3().bucket();
            case HOST_DOCUMENT -> awsProperties.s3().hostDocumentBucket();
        };
    }
}
