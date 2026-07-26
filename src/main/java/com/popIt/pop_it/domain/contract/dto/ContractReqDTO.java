package com.popIt.pop_it.domain.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ContractReqDTO {
    public record ContractSignatureReq(
            @NotBlank
            @Pattern(regexp = "^https://.+", message = "유효한 URL이어야 합니다.")
            String signatureUrl
    ){}
}
