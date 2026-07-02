package com.popIt.pop_it.domain.pass.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Telecom {
    SKT("SKT"),
    KT("KT"),
    LGU("LG U+"),
    SKT_MVNO("SKT 알뜰폰"),
    KT_MVNO("KT 알뜰폰"),
    LGU_MVNO("LG U+ 알뜰폰");

    private final String description;
}
