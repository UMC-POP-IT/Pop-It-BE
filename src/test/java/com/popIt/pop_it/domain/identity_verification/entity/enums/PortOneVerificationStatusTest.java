package com.popIt.pop_it.domain.identity_verification.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PortOneVerificationStatusTest {

    @Test
    @DisplayName("포트원 원본 값이 정확히 VERIFIED면 VERIFIED로 판정된다")
    void from_verified_returnsVerified() {
        assertThat(PortOneVerificationStatus.from("VERIFIED"))
                .isEqualTo(PortOneVerificationStatus.VERIFIED);
    }

    @ParameterizedTest
    @DisplayName("VERIFIED가 아닌 값(실패/대기/알 수 없는 값/대소문자 다름)은 예외 없이 OTHER로 판정된다")
    @ValueSource(strings = {"FAILED", "PENDING", "UNKNOWN_STATUS", "verified", "Verified", " VERIFIED", "VERIFIED "})
    void from_nonVerifiedValues_returnOtherWithoutException(String raw) {
        assertThat(PortOneVerificationStatus.from(raw))
                .isEqualTo(PortOneVerificationStatus.OTHER);
    }

    @ParameterizedTest
    @DisplayName("null이거나 빈 문자열이어도 예외 없이 OTHER로 판정된다")
    @NullAndEmptySource
    void from_nullOrEmpty_returnOtherWithoutException(String raw) {
        assertThat(PortOneVerificationStatus.from(raw))
                .isEqualTo(PortOneVerificationStatus.OTHER);
    }
}
