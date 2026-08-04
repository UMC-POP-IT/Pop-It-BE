package com.popIt.pop_it.domain.upload.dto;

import com.popIt.pop_it.domain.upload.enums.UploadType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public class UploadReqDTO {

    public record PresignedUrlReq(
            @Schema(description = "업로드 용도. 용도에 따라 저장 버킷/경로가 달라짐", example = "SPACE_IMAGE")
            @NotNull UploadType uploadType,

            @Schema(description = "업로드할 파일 정보 목록 (최대 10개)")
            @NotEmpty @Size(max = 10, message = "한 번에 최대 10개의 파일만 업로드할 수 있습니다.") @Valid List<UploadFileInfoReq> files
    ) {}

    public record UploadFileInfoReq(
            @NotBlank
            @Schema(description = "파일 MIME 타입", allowableValues = {"image/jpeg", "image/png", "application/pdf"})
            @Pattern(regexp = "image/jpeg|image/png|application/pdf")
            String contentType
    ) {}
}
