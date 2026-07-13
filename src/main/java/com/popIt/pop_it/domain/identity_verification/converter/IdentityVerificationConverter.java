package com.popIt.pop_it.domain.identity_verification.converter;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;

import java.util.Objects;

public class IdentityVerificationConverter {
    public static IdentityVerificationResDTO.Verify toVerify(IdentityVerification verification) {

        return IdentityVerificationResDTO.Verify.builder()
                .isVerified(Objects.equals(verification.getStatus(), "VERIFIED"))
                .verifiedAt(verification.getVerifiedAt())
                .build();
    }

}
