package com.popIt.pop_it.domain.reservation.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

public class ReservationException extends ProjectException {
    public ReservationException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
