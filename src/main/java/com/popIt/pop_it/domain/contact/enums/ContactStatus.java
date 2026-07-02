package com.popIt.pop_it.domain.contact.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContactStatus {

    PENDING_SIGNATURE("서명대기"),
    COMPLETED("완료")
    ;

    private final String description;
}
