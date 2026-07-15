package com.popIt.pop_it.domain.contract.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

public class ContractException extends ProjectException {
    public ContractException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
