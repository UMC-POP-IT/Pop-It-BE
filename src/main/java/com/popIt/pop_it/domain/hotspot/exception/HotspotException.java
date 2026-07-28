package com.popIt.pop_it.domain.hotspot.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

public class HotspotException extends ProjectException {
    public HotspotException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
