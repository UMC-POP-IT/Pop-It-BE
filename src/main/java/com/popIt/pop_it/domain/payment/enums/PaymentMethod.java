package com.popIt.pop_it.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    CARD("카드"),
    BANK_TRANSFER("계좌이체"),
    KAKAO_PAY("카카오페이"),
    NAVER_PAY("네이버페이"),
    VIRTUAL_ACCOUNT("가상계좌"),
    MOBILE_PAYMENT("휴대폰 결제"),
    TOSS_PAY("토스페이")
    ;

    private final String description;
}
