package com.popIt.pop_it.domain.payment.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * @see <a href="https://docs.tosspayments.com/reference/error-codes#결제-승인">결제 승인 에러코드 전체 목록</a>
 */
@Getter
@RequiredArgsConstructor
public class TossErrorCode implements BaseErrorCode {

    private final HttpStatus status;
    private final String code;
    private final String message;
}
