package com.popIt.pop_it.domain.scene.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SceneErrorCode implements BaseErrorCode {

    SCENE_NOT_FOUND(HttpStatus.NOT_FOUND, "CURATION_SCENE404_1", "해당 씬을 찾을 수 없습니다."),
    SCENE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CURATION_SCENE403_1", "본인 소유 공간이 아니어서 처리할 수 없습니다."),
    SCENE_REFERENCED_BY_HOTSPOT(HttpStatus.CONFLICT, "CURATION_SCENE409_1", "다른 씬의 핫스팟이 참조 중이라 삭제할 수 없습니다."),
    SCENE_DEFAULT_ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "CURATION_SCENE409_2", "동시에 기본 씬으로 지정하는 요청이 있어 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
