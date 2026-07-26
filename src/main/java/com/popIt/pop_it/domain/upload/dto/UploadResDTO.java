package com.popIt.pop_it.domain.upload.dto;

import java.util.List;

public class UploadResDTO {
    public record PresignedUrlListRes(
            List<PresignedUrlInfoRes> uploads
    ) {}

    public record PresignedUrlInfoRes(
            String presignedUrl,
            String fileUrl
    ) {}
}
