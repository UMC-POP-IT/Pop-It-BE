package com.popIt.pop_it.domain.hotspot.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HotspotErrorCode implements BaseErrorCode {

    HOTSPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "CURATION404_2", "해당 핫스팟을 찾을 수 없습니다."),
    HOTSPOT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CURATION403_1", "본인 소유 공간이 아니어서 처리할 수 없습니다."),
    HOTSPOT_TYPE_FIELD_MISMATCH(HttpStatus.BAD_REQUEST, "CURATION400_2", "type 값에 맞는 필드(INFO→description, LINK→targetSceneId)가 없거나 맞지 않습니다."),
    HOTSPOT_TARGET_SCENE_NOT_FOUND(HttpStatus.NOT_FOUND, "CURATION404_1", "targetSceneId에 해당하는 씬을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
