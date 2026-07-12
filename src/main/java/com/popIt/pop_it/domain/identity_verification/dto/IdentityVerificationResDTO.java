package com.popIt.pop_it.domain.identity_verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.popIt.pop_it.domain.identity_verification.entity.enums.Gender;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

public class IdentityVerificationResDTO {

    @Builder
    public record Verify (
            Boolean isVerified,
            LocalDateTime verifiedAt
    ){}

    @Builder
    public record PortOneIdentityVerification (
            @JsonProperty("id") String identityVerificationId,
            String status,
            Instant verifiedAt,
            // 실제 인증 정보
            VerifiedCustomer verifiedCustomer
    ) {
        @Builder
        public record VerifiedCustomer(
                String name,
                String phoneNumber,
                String birthDate,
                Gender gender,
                String ci
        ) {}
    }

    @Builder
    public record PortOneError (
            String type,
            String message
    ) {}


}
