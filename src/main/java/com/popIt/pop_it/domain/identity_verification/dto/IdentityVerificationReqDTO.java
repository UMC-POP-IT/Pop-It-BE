package com.popIt.pop_it.domain.identity_verification.dto;

import jakarta.validation.constraints.NotBlank;

public class IdentityVerificationReqDTO {

    public record Verify (
            @NotBlank
            String identityVerificationId
    ){}
}
