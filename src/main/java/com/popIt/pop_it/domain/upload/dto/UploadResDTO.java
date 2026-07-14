package com.popIt.pop_it.domain.upload.dto;

import java.util.List;

public class UploadResDTO {
    public record PresignedUrlList(
            List<PresignedUrlInfo> uploads
    ) {}

    public record PresignedUrlInfo(
            String presignedUrl,
            String fileUrl
    ) {}
}
