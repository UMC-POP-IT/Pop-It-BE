package com.popIt.pop_it.domain.upload.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadErrorCode implements BaseErrorCode {
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
