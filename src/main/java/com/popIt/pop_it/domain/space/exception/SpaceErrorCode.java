package com.popIt.pop_it.domain.space.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SpaceErrorCode implements BaseErrorCode {

    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "SPACE404_1", "해당 공간을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
