package com.popIt.pop_it.domain.hotspot.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HotspotSuccessCode implements BaseSuccessCode {

    HOTSPOT_CREATED(HttpStatus.CREATED, "CURATION201_2", "핫스팟이 생성되었습니다."),
    HOTSPOT_UPDATED(HttpStatus.OK, "CURATION200_6", "핫스팟이 수정되었습니다."),
    HOTSPOT_DELETED(HttpStatus.OK, "CURATION200_7", "핫스팟이 삭제되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
