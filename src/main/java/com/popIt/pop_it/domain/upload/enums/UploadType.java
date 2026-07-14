package com.popIt.pop_it.domain.upload.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadType {
    SPACE_IMAGE("space"),
    BUSINESS_LICENSE("business-license"),
    BANKBOOK("bankbook"),
    SIGNATURE("signature"),
    CHECKOUT("checkout");

    private final String path;
}
