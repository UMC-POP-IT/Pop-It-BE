package com.popIt.pop_it.domain.identity_verification.converter;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.identity_verification.entity.enums.PortOneVerificationStatus;

public class IdentityVerificationConverter {
    public static IdentityVerificationResDTO.IdentityVerificationVerifyRes toVerify(IdentityVerification verification) {

        // 본인인증 이력이 없거나(verification == null), 있어도 VERIFIED가 아니면 미인증으로 취급.
        boolean isVerified = verification != null && verification.getStatus() == PortOneVerificationStatus.VERIFIED;

        return IdentityVerificationResDTO.IdentityVerificationVerifyRes.builder()
                .isVerified(isVerified)
                .verifiedAt(isVerified ? verification.getVerifiedAt() : null)
                .build();
    }

}
