package com.popIt.pop_it.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PENDING("결제 대기"),
    PAID("결제 완료"),
    FAILED("결제 실패"),
    EXPIRED("결제 만료")
    ;

    private final String description;
}
