package com.popIt.pop_it.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementStepStatus {

    PENDING("대기중"),
    PROCESSING("처리중"),
    DONE("완료"),
    FAILED("실패")
    ;

    private final String description;
}
