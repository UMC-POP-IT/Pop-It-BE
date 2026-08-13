package com.popIt.pop_it.domain.space.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BuildingType {

    LARGE_OFFICE("대형 사무실"),
    SMALL_MEDIUM_OFFICE("중소형 사무실"),
    OFFICETEL("오피스텔형"),
    COMPLEX_COMMERCIAL("단지내 상가"),
    GENERAL_COMMERCIAL("일반 상가"),
    MIXED_USE_COMMERCIAL("복합 상가")
    ;

    private final String description;
}
