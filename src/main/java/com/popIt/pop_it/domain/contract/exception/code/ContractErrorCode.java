package com.popIt.pop_it.domain.contract.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public class ContractErrorCode implements BaseErrorCode {

    private final HttpStatus status;
    private final String code;
    private final String message;
}
