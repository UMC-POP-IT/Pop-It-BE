package com.popIt.pop_it.domain.upload.dto;

import com.popIt.pop_it.domain.upload.enums.UploadType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UploadReqDTO {

    public record PresignedUrl(
            @NotNull UploadType uploadType,
            @NotEmpty @Valid List<FileInfo> files
    ) {}

    public record FileInfo(
            @NotBlank String contentType
    ) {}
}
