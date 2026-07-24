package com.popIt.pop_it.domain.upload.dto;

import com.popIt.pop_it.domain.upload.enums.UploadType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UploadReqDTO {

    public record PresignedUrlReq(
            @NotNull UploadType uploadType,
            @NotEmpty @Size(max = 10, message = "한 번에 최대 10개의 파일만 업로드할 수 있습니다.") @Valid List<FileInfoReq> files
    ) {}

    public record FileInfoReq(
            @NotBlank
            @Schema(description = "파일 MIME 타입", allowableValues = {"image/jpeg", "image/png", "application/pdf"})
            String contentType
    ) {}
}
