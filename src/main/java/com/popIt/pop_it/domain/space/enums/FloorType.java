package com.popIt.pop_it.domain.space.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FloorType {

    GENERAL_FLOOR("일반 층"),
    SEMI_BASEMENT("반지층"),
    BASEMENT("지하"),
    ROOFTOP("옥탑")
    ;

    private final String description;
}
