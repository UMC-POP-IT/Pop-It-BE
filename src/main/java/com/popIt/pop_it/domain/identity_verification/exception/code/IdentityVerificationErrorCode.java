package com.popIt.pop_it.domain.identity_verification.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IdentityVerificationErrorCode implements BaseErrorCode {
    NOT_VERIFIED(HttpStatus.CONFLICT, "IDENTITY_VERIFICATION409_1", "본인인증에 실패했습니다."),
    PORTONE_API_ERROR(HttpStatus.BAD_GATEWAY, "IDENTITY_VERIFICATION502_1", "포트원 인증 조회 API가 실패했습니다.");

//    IDENTITY_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "IDENTITY_VERIFICATION404_1", "본인인증 내역을 찾을 수 없습니다."),
//    ALREADY_PROCESSED(HttpStatus.CONFLICT, "IDENTITY_VERIFICATION409_2", "이미 처리된 본인인증 건입니다."),
//    AGE_RESTRICTION(HttpStatus.FORBIDDEN, "IDENTITY_VERIFICATION403_1", "본인인증 최소 연령 조건을 만족하지 않습니다."),
//    DUPLICATE_IDENTITY(HttpStatus.CONFLICT, "IDENTITY_VERIFICATION409_3", "이미 가입된 본인인증 정보입니다.");
//
    private final HttpStatus status;
    private final String code;
    private final String message;
}
