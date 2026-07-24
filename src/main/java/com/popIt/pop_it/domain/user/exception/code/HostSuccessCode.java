package com.popIt.pop_it.domain.user.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HostSuccessCode implements BaseSuccessCode {

    HOST_REGISTER(HttpStatus.CREATED, "HOST201_1", "호스트 등록이 완료되었습니다."),
    HOST_GET(HttpStatus.OK, "USER200_2", "호스트 조회에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
