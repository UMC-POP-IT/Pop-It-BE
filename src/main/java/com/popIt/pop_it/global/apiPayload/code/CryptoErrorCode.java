package com.popIt.pop_it.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CryptoErrorCode implements BaseErrorCode {

    ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO500_1", "암호화에 실패했습니다."),
    DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO500_2", "복호화에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
