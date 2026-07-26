package com.popIt.pop_it.domain.hotspot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HotspotType {

    INFO("설명 텍스트를 보여주는 핫스팟"),
    LINK("다른 씬(방)으로 이동하는 핫스팟"),
    ;

    private final String description;
}
