package com.popIt.pop_it.domain.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ContractReqDTO {
    public record ContractSignatureReq(
            @Schema(description = "전자서명 이미지 URL(S3)", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/signature/10/uuid.png")
            @NotBlank
            @Pattern(regexp = "^https://.+", message = "유효한 URL이어야 합니다.")
            String signatureUrl
    ){}
}
