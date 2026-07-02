package com.popIt.pop_it.domain.terms.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermCode {
    SERVICE("서비스 이용약관"),
    PRIVACY("개인정보 처리방침"),
    MARKETING("마케팅 수신 동의");

    private final String description;
}
