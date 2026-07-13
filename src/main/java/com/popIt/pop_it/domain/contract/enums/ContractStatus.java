package com.popIt.pop_it.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractStatus {

    PENDING_SIGNATURE("서명 대기"),
    PENDING_PAYMENT("결제 대기"), // 서명 완료
    COMPLETED("결제 완료")
    ;

    private final String description;
}
