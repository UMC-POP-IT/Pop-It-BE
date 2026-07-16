package com.popIt.pop_it.domain.identity_verification.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IdentityVerificationErrorCode implements BaseErrorCode {
    NOT_VERIFIED(HttpStatus.CONFLICT, "IDENTITY_VERIFICATION409_1", "본인인증에 실패했습니다."),
    PORTONE_API_ERROR(HttpStatus.BAD_GATEWAY, "IDENTITY_VERIFICATION502_1", "포트원 인증 조회 API가 실패했습니다."),

    // 같은 인증 시도(identityVerificationId) 재사용
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "IDENTITY_VERIFICATION409_2", "이미 처리된 본인인증 건입니다."),
    // 다른 인증 시도지만 같은 사람
    DUPLICATE_IDENTITY(HttpStatus.CONFLICT, "IDENTITY_VERIFICATION409_3", "이미 가입된 본인인증 정보입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
