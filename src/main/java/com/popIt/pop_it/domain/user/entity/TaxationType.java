package com.popIt.pop_it.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaxationType {
    SIMPLIFIED("간이과세자"),
    GENERAL("일반과세자");

    private final String description;
}
