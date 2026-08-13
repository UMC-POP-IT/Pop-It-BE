package com.popIt.pop_it.domain.upload.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class UploadResDTO {
    public record PresignedUrlListRes(
            @Schema(description = "요청한 파일 순서대로 발급된 presigned URL 정보 목록")
            List<PresignedUrlInfoRes> uploads
    ) {}

    public record PresignedUrlInfoRes(
            @Schema(description = "S3에 파일을 업로드할 때 PUT 요청을 보낼 URL. 10분간만 유효하며 다른 용도로 저장하지 않음", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid.jpg?X-Amz-...")
            String presignedUrl,

            @Schema(description = "업로드 완료 후 실제로 저장/전달해야 할 파일 URL. presignedUrl로 PUT 업로드가 끝나면 이 값을 다른 API의 imageUrls 등에 담아 전달", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid.jpg")
            String fileUrl
    ) {}
}
