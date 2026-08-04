package com.popIt.pop_it.domain.identity_verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class IdentityVerificationReqDTO {
    public record IdentityVerificationVerifyReq(
            @Schema(description = "포트원 본인인증 완료 후 발급된 인증 ID", example = "identity-verification-...")
            @NotBlank
            String identityVerificationId
    ){}
}
