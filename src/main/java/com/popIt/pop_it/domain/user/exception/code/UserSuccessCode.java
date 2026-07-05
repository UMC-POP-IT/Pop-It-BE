package com.popIt.pop_it.domain.user.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements BaseErrorCode {

    USER_LOGIN(HttpStatus.OK, "MEMBER200_1", "성공적으로 로그인 했습니다. ");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
