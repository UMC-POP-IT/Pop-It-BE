package com.popIt.pop_it.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthReqDTO {
    public record TokenReissueReq(
            @NotBlank(message = "refreshToken은 필수입니다.")
            String refreshToken
    ) {}
}
