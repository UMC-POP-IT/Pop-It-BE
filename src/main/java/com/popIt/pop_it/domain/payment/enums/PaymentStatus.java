package com.popIt.pop_it.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    READY("결제 대기"),
    PAID("결제 완료"),
    FAILED("결제 실패")
    ;

    private final String description;
}
