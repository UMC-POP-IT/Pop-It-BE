package com.popIt.pop_it.domain.payment.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentSuccessCode implements BaseSuccessCode {

    PAYMENT_PREPARED(HttpStatus.CREATED, "PAYMENT200_1", "결제 준비가 완료되었습니다."),
    PAYMENT_CONFIRM(HttpStatus.CREATED, "PAYMENT200_2", "결제가 완료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
