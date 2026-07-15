package com.popIt.pop_it.domain.payment.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT404_1", "결제를 찾을 수 없습니다."),
    PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "PAYMENT403_1", "본인의 계약만 결제할 수 있습니다."),
    PAYMENT_CONFLICT_PAYMENT(HttpStatus.CONFLICT, "PAYMENT409_1", "결제 가능한 계약 상태가 아닙니다."),
    PAYMENT_CONFLICT_KEY(HttpStatus.CONFLICT, "PAYMENT409_2", "동일한 Idempotency-Key로 다른 요청이 이미 처리되었습니다."),
    PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "PAYMENT409_3", "이미 결제가 완료되었습니다."),
    PAYMENT_RETRYABLE(HttpStatus.CONFLICT, "PAYMENT409_4", "이전 결제 시도가 실패했습니다. 새로운 Idempotency-Key로 다시 요청해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
