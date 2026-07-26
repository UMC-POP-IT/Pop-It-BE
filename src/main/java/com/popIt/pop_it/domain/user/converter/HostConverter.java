package com.popIt.pop_it.domain.user.converter;

import com.popIt.pop_it.domain.user.dto.HostProfileRes;
import com.popIt.pop_it.domain.user.dto.HostRegisterReq;
import com.popIt.pop_it.domain.user.dto.HostRegisterRes;
import com.popIt.pop_it.domain.user.entity.HostProfile;

public class HostConverter {

    // 요청 DTO → HostProfile 엔티티 (민감 필드는 평문 세팅, 암호화는 CryptoConverter에 위임)
    // businessRegistrationNumberHash는 빈(CryptoService) 의존이 필요해 Service에서 계산 후 주입받는다.
    public static HostProfile toHostProfile(Long userId, HostRegisterReq request, String businessRegistrationNumberHash) {
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

    public static HostRegisterRes toRegisterResponse(HostProfile hostProfile) {
        return new HostRegisterRes(hostProfile.getId(), hostProfile.getCreatedAt());
    }

    public static HostProfileRes toProfileResponse(HostProfile hostProfile) {
        return new HostProfileRes(
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
