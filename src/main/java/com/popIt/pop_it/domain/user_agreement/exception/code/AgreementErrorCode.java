package com.popIt.pop_it.domain.user_agreement.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AgreementErrorCode implements BaseErrorCode {

    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "AGREEMENT404_1", "존재하지 않는 약관이 포함되어 있습니다."),
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST, "AGREEMENT400_1", "필수 약관에는 반드시 동의해야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
