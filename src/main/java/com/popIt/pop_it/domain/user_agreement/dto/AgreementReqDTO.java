package com.popIt.pop_it.domain.user_agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AgreementReqDTO {

    // 약관 개수가 하나일 수도 여러 개일 수도 있어, 항상 리스트로 받아 확장성 확보
    public record Save(
            @Schema(description = "동의할 약관 목록")
            @NotEmpty(message = "동의 항목은 최소 1개 이상이어야 합니다.")
            @Valid
            List<Item> agreements
    ) {}

    public record Item(
            @Schema(description = "약관 ID", example = "1")
            @NotNull(message = "약관 ID는 필수입니다.")
            Long termId,

            @Schema(description = "동의 여부", example = "true")
            @NotNull(message = "동의 여부는 필수입니다.")
            Boolean isAgreed
    ) {}
}
