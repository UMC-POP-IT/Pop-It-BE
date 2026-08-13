package com.popIt.pop_it.domain.identity_verification.entity.enums;

public enum PortOneVerificationStatus {
    VERIFIED,
    OTHER; // FAILED, PENDING 등 포트원이 줄 가능성 있는 상태

    public static PortOneVerificationStatus from(String raw) {
        return "VERIFIED".equals(raw) ? VERIFIED : OTHER;
    }
}
