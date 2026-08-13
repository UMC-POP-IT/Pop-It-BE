package com.popIt.pop_it.domain.identity_verification.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

public class IdentityVerificationException extends ProjectException {
    public IdentityVerificationException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    public IdentityVerificationException(BaseErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
