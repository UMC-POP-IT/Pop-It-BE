package com.popIt.pop_it.domain.space.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpaceCategory {

    POPUP_STORE("팝업스토어"),
    EXHIBITION_GALLERY("전시/갤러리"),
    COMPLEX_SPACE("복합공간"),
    SHOWROOM("쇼룸"),
    CAFE_FNB("카페/F&B")
    ;

    private final String description;
}
