package com.popIt.pop_it.domain.upload.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadType {
    SPACE_IMAGE("space");

    private final String path;
}
