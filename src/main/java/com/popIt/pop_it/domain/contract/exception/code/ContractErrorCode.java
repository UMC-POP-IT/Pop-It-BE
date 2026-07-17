package com.popIt.pop_it.domain.contract.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContractErrorCode implements BaseErrorCode {

    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTRACT404_1", "계약을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
