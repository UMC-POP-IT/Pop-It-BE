package com.popIt.pop_it.domain.scene.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

public class SceneException extends ProjectException {
    public SceneException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
