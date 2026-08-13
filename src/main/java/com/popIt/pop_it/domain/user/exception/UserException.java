package com.popIt.pop_it.domain.user.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

public class UserException extends ProjectException {
    public UserException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
