package com.popIt.pop_it.domain.identity_verification.converter;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;

import java.util.Objects;

public class IdentityVerificationConverter {
    public static IdentityVerificationResDTO.IdentityVerificationVerifyRes toVerify(IdentityVerification verification) {

        if (verification == null) { // 본인인증 한적 없는 경우
            return IdentityVerificationResDTO.IdentityVerificationVerifyRes.builder()
                    .isVerified(false)
                    .verifiedAt(null)
                    .build();
        }

        return IdentityVerificationResDTO.IdentityVerificationVerifyRes.builder()
                .isVerified(Objects.equals(verification.getStatus(), "VERIFIED"))
                .verifiedAt(verification.getVerifiedAt())
                .build();
    }

}
