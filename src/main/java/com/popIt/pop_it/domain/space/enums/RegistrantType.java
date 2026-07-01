package com.popIt.pop_it.domain.space.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegistrantType {

    OWNER("소유자")
    ;

    private final String description;
}
