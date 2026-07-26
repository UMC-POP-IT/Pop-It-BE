package com.popIt.pop_it.domain.user.converter;

import com.popIt.pop_it.domain.user.dto.HostProfileResponse;
import com.popIt.pop_it.domain.user.dto.HostRegisterRequest;
import com.popIt.pop_it.domain.user.dto.HostRegisterResponse;
import com.popIt.pop_it.domain.user.entity.HostProfile;

public class HostConverter {

    // 요청 DTO → HostProfile 엔티티 (민감 필드는 평문 세팅, 암호화는 CryptoConverter에 위임)
    // businessRegistrationNumberHash는 빈(CryptoService) 의존이 필요해 Service에서 계산 후 주입받는다.
    public static HostProfile toHostProfile(Long userId, HostRegisterRequest request, String businessRegistrationNumberHash) {
        return HostProfile.builder()
                .userId(userId)
                .taxationType(request.taxationType())
                .businessRegistrationNumber(request.normalizedBusinessRegistrationNumber())
                .businessRegistrationNumberHash(businessRegistrationNumberHash)
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

    public static HostProfileResponse toProfileResponse(HostProfile hostProfile) {
        return new HostProfileResponse(
                hostProfile.getId(),
                hostProfile.getTaxationType(),
                hostProfile.getBusinessRegistrationNumber(),
                hostProfile.getBusinessLicenseUrl(),
                hostProfile.getBusinessName(),
                hostProfile.getBusinessAddress(),
                hostProfile.getBankbookCopyUrl(),
                hostProfile.getBank(),
                hostProfile.getSettlementAccountNumber(),
                hostProfile.getAccountHolder(),
                hostProfile.getCreatedAt(),
                hostProfile.getUserId()
        );
    }
}
