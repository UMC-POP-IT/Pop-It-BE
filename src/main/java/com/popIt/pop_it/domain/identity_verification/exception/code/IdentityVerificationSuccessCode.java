package com.popIt.pop_it.domain.identity_verification.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IdentityVerificationSuccessCode implements BaseSuccessCode {

    VERIFIED(HttpStatus.CREATED, "IDENTITY_VERIFICATION201_1", "본인인증이 완료되었습니다."),
    IS_VERIFIED(HttpStatus.OK, "IDENTITY_VERIFICATION200_1", "본인인증 상태 조회가 완료되었습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
