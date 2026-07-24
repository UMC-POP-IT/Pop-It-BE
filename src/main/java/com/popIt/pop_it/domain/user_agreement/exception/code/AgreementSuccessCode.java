package com.popIt.pop_it.domain.user_agreement.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AgreementSuccessCode implements BaseSuccessCode {

    AGREEMENT_SAVED(HttpStatus.OK, "AGREEMENT200_1", "약관 동의가 저장되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
