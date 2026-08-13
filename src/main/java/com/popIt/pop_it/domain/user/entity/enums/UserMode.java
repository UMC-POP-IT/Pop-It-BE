package com.popIt.pop_it.domain.user.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserMode {
    HOST("호스트"),
    GUEST("게스트");

    private final String description;
}
