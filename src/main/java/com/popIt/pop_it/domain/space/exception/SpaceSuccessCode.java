package com.popIt.pop_it.domain.space.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SpaceSuccessCode implements BaseSuccessCode {

    SPACE_CREATED(HttpStatus.CREATED, "SPACE201_1", "공간 등록에 성공했습니다."),
    AI_RECOMMENDED_SPACE_LIST(HttpStatus.OK, "SPACE200_5", "AI 맞춤 추천 공간 조회에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
