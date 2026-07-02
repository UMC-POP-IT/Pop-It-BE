package com.popIt.pop_it.domain.facility.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityCategory {

    HEATING_COOLING("냉난방"),
    SECURITY("보안"),
    ETC("기타")
    ;

    private final String description;
}
