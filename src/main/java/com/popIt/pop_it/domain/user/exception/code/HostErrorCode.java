package com.popIt.pop_it.domain.user.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HostErrorCode implements BaseErrorCode {

    HOST_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "HOST409_1", "이미 등록된 호스트 프로필입니다."),
    BUSINESS_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "HOST409_2", "이미 등록된 사업자등록번호입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
