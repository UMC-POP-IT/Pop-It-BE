package com.popIt.pop_it.domain.user.converter;

import com.popIt.pop_it.domain.user.dto.HostRegisterRequest;
import com.popIt.pop_it.domain.user.dto.HostRegisterResponse;
import com.popIt.pop_it.domain.user.entity.HostProfile;

public class HostConverter {

    // 요청 DTO → HostProfile 엔티티 (민감 필드는 평문 세팅, 암호화는 CryptoConverter에 위임)
    public static HostProfile toHostProfile(Long userId, HostRegisterRequest request) {
        return HostProfile.builder()
                .userId(userId)
                .taxationType(request.taxationType())
                .businessRegistrationNumber(request.normalizedBusinessRegistrationNumber())
                .businessLicenseUrl(request.businessLicenseUrl())
                .businessName(request.businessName())
                .businessAddress(request.businessAddress())
                .bank(request.bank())
                .settlementAccountNumber(request.settlementAccountNumber())
                .accountHolder(request.accountHolder())
                .bankbookCopyUrl(request.bankbookCopyUrl())
                .build();
    }

    public static HostRegisterResponse toRegisterResponse(HostProfile hostProfile) {
        return new HostRegisterResponse(hostProfile.getId(), hostProfile.getCreatedAt());
    }
}
