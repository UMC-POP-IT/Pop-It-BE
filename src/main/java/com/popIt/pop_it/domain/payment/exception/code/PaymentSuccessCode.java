package com.popIt.pop_it.domain.payment.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentSuccessCode implements BaseSuccessCode {

    PAYMENT_PREPARED(HttpStatus.CREATED, "PAYMENT201_1", "결제 준비가 완료되었습니다."),
    PAYMENT_CONFIRM(HttpStatus.OK, "PAYMENT200_1", "결제가 완료되었습니다."),
    PAYMENT_WEBHOOK_RECEIVED(HttpStatus.OK, "PAYMENT200_2", "웹훅을 수신했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
