package com.popIt.pop_it.domain.contract.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContractSuccessCode implements BaseSuccessCode {

    CONTRACT_INFO_FOUND(HttpStatus.OK, "CONTRACT200_1", "계약 예정 정보를 조회했습니다."),
    SIGNATURE_SUCCESS(HttpStatus.CREATED, "CONTRACT201_1", "전자서명이 완료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
