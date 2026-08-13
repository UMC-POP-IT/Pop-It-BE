package com.popIt.pop_it.domain.reservation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {

    PENDING_APPROVAL("승인대기"),
    APPROVED("승인 완료"),
    CONTRACT_COMPLETED("계약완료"), // 결제 전
    PAYMENT_COMPLETED("결제완료"), // 이용 전
    IN_USE("사용중"),
    USAGE_COMPLETED("이용 완료"),
    CHECKOUT_COMPLETED("퇴실 완료"),
    CANCELLED("예약 취소")
    ;

    private final String description;
}
