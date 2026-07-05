package com.popIt.pop_it.global.apiPayload.code;

import org.springframework.http.HttpStatus;

public interface BaseSuccessCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
