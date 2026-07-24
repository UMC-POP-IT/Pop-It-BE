package com.popIt.pop_it.domain.space.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SpaceSuccessCode implements BaseSuccessCode {

    SPACE_CREATED(HttpStatus.CREATED, "SPACE201_1", "공간 등록에 성공했습니다."),
    MY_PAGE_LIST_FETCHED(HttpStatus.OK, "SPACE200_1", "내 공간 목록 조회에 성공했습니다."),
    SPACE_DETAIL_FETCHED(HttpStatus.OK, "SPACE200_2", "공간 상세 조회에 성공했습니다."),
    SPACE_SEARCH_FETCHED(HttpStatus.OK, "SPACE200_3", "공간 탐색에 성공했습니다."),
    SPACE_UPDATED(HttpStatus.OK, "SPACE200_4", "공간 수정에 성공했습니다."),
    AI_RECOMMENDED_SPACE_LIST(HttpStatus.OK, "SPACE200_5", "AI 맞춤 추천 공간 조회에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
