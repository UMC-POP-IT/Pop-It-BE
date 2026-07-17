package com.popIt.pop_it.domain.upload.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UploadSuccessCode implements BaseSuccessCode {
    PRESIGNED_URL_ISSUED(HttpStatus.OK, "UPLOAD200_1", "presigned URL 발급에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
