package com.popIt.pop_it.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthReqDTO {
    public record TokenReissueReq(
            @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
            @NotBlank(message = "refreshToken은 필수입니다.")
            String refreshToken
    ) {}

    public record Exchange(
            @Schema(description = "소셜 로그인 리다이렉트로 전달받은 1회용 코드 (발급 후 30초 이내 유효)", example = "a1b2c3d4-...")
            @NotBlank(message = "code는 필수입니다.")
            String code,

            @Schema(description = "로그인 시작 시 프론트가 생성해뒀던 원본 verifier 값 (challenge의 원본, 해시값 아님)", example = "test123")
            @NotBlank(message = "verifier는 필수입니다.")
            String verifier
    ) {}
}
