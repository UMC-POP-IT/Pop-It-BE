package com.popIt.pop_it.domain.upload.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadErrorCode implements BaseErrorCode {
    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "UPLOAD400_1", "지원하지 않는 파일 형식입니다."),
    TOO_MANY_FILES(HttpStatus.BAD_REQUEST, "UPLOAD400_2", "요청 가능한 파일 개수를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
