package com.popIt.pop_it.domain.user_agreement.converter;

import com.popIt.pop_it.domain.user_agreement.dto.AgreementResDTO;
import com.popIt.pop_it.domain.user_agreement.entity.UserAgreement;

import java.time.LocalDateTime;
import java.util.List;

public class AgreementConverter {

    // 신규 동의 이력 엔티티 생성 (upsert의 insert 경로)
    public static UserAgreement toEntity(Long userId, Long termId, boolean isAgreed, LocalDateTime agreedAt) {
        return UserAgreement.builder()
                .userId(userId)
                .termId(termId)
                .isAgreed(isAgreed)
                .agreedAt(agreedAt)
                .build();
    }

    public static AgreementResDTO.SaveResult toSaveResult(List<UserAgreement> agreements) {
        List<AgreementResDTO.AgreedItem> items = agreements.stream()
                .map(a -> new AgreementResDTO.AgreedItem(a.getTermId(), a.isAgreed(), a.getAgreedAt()))
                .toList();
        return new AgreementResDTO.SaveResult(items);
    }
}
