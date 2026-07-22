package com.popIt.pop_it.domain.scene.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SceneErrorCode implements BaseErrorCode {

    SCENE_NOT_FOUND(HttpStatus.NOT_FOUND, "CURATION404_1", "해당 씬을 찾을 수 없습니다."),
    SCENE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CURATION403_1", "본인 소유 공간이 아니어서 처리할 수 없습니다."),
    SCENE_REFERENCED_BY_HOTSPOT(HttpStatus.CONFLICT, "CURATION409_1", "다른 씬의 핫스팟이 참조 중이라 삭제할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
