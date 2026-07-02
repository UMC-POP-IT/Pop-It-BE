package com.popIt.pop_it.domain.user.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Bank {
    KB("국민은행"),
    WOORI("우리은행"),
    SHINHAN("신한은행"),
    HANA("하나은행"),
    NH("농협은행"),
    IBK("기업은행"),
    KAKAO_BANK("카카오뱅크"),
    TOSS_BANK("토스뱅크");

    private final String description;
}
