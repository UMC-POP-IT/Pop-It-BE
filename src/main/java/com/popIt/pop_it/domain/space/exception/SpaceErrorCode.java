package com.popIt.pop_it.domain.space.exception;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SpaceErrorCode implements BaseErrorCode {

    INVALID_AVAILABLE_DATE_RANGE(HttpStatus.BAD_REQUEST, "SPACE400_1", "계약 가능 시작일은 종료일보다 늦을 수 없습니다."),
    FACILITY_NOT_FOUND(HttpStatus.BAD_REQUEST, "SPACE400_2", "존재하지 않는 시설이 포함되어 있습니다."),
    INVALID_COORDINATE_PAIR(HttpStatus.BAD_REQUEST, "SPACE400_3", "위도와 경도는 함께 수정해야 합니다."),
    SPACE_HAS_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "SPACE400_4", "진행 중인 예약이 있어 공간을 삭제할 수 없습니다."),
    HOST_PROFILE_REQUIRED(HttpStatus.FORBIDDEN, "SPACE403_1", "호스트 권한이 없습니다. 호스트 등록을 먼저 완료해주세요."),
    NOT_SPACE_OWNER(HttpStatus.FORBIDDEN, "SPACE403_2", "본인이 등록한 공간만 수정하거나 삭제할 수 있습니다."),
    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "SPACE404_1", "해당 공간을 찾을 수 없습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "SPACE400_3", "잘못된 커서 값입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
