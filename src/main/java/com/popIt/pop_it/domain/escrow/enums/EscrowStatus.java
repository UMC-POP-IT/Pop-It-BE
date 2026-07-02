package com.popIt.pop_it.domain.escrow.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EscrowStatus {

    PENDING("대기중"),
    DEPOSITED("입금 완료"),
    RELEASED_TO_HOST("호스트 정산 완료"),
    REFUNDED("환불 완료"),
    DISPUTED("분쟁중")
    ;

    private final String description;
}
