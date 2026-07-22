package com.popIt.pop_it.domain.scene.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SceneSuccessCode implements BaseSuccessCode {

    SCENE_LIST(HttpStatus.OK, "CURATION200_1", "씬 목록이 조회되었습니다."),
    SCENE_DETAIL(HttpStatus.OK, "CURATION200_2", "씬 상세 정보가 조회되었습니다."),
    SCENE_CREATED(HttpStatus.CREATED, "CURATION201_1", "씬이 생성되었습니다."),
    SCENE_UPDATED(HttpStatus.OK, "CURATION200_3", "씬이 수정되었습니다."),
    SCENE_DELETED(HttpStatus.OK, "CURATION200_4", "씬이 삭제되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
