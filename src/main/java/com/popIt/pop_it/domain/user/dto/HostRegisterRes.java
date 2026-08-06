package com.popIt.pop_it.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record HostRegisterRes(
        @Schema(description = "생성된 호스트 프로필 ID", example = "1")
        Long id,

        @Schema(description = "호스트 등록 일시", example = "2026-08-04T10:30:00")
        LocalDateTime createdAt
) {}
