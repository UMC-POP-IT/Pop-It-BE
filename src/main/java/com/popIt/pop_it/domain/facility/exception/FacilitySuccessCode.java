package com.popIt.pop_it.domain.facility.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FacilitySuccessCode implements BaseSuccessCode {

    FACILITY_LIST_FETCHED(HttpStatus.OK, "FACILITY200_1", "시설 목록 조회에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
