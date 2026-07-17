package com.popIt.pop_it.domain.contract.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractStatus {

    HOST_SIGNATURE_PENDING("호스트 서명대기"), // 호스트가 서명하면 GUEST_PENDING_SIGNATURE로 전이
    GUEST_SIGNATURE_PENDING("게스트 서명대기"), // 게스트가 서명하면 COMPLETED로 전이
    COMPLETED("완료")
    ;

    private final String description;
}
