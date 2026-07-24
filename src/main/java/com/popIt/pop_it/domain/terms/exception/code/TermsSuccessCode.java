package com.popIt.pop_it.domain.terms.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TermsSuccessCode implements BaseSuccessCode {

    TERMS_LIST_FETCHED(HttpStatus.OK, "TERMS200_1", "약관 목록 조회에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
