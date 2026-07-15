package com.popIt.pop_it.domain.user.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements BaseSuccessCode {

    USER_LOGIN(HttpStatus.OK, "USER200_1", "성공적으로 로그인 했습니다."),
    USER_LOGOUT(HttpStatus.OK, "USER200_2", "성공적으로 로그아웃 했습니다."),
    USER_REISSUE(HttpStatus.OK, "USER200_3", "성공적으로 액세스 토큰을 재발급 했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
