package com.popIt.pop_it.domain.reservation.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReservationSuccessCode implements BaseSuccessCode {

    RESERVATION_LIST(HttpStatus.OK, "RESERVATION200_1", "예약 목록이 조회되었습니다."),
    RESERVATION_OK(HttpStatus.OK, "RESERVATION200_2", "예약을 승인했습니다."),
    RESERVATION_NO(HttpStatus.OK, "RESERVATION200_3", "예약을 거절했습니다."),
    RESERVATION_GUEST_CANCEL(HttpStatus.OK, "RESERVATION200_4", "예약이 취소되었습니다."),
    RESERVATION_HOST_CANCEL(HttpStatus.OK, "RESERVATION200_5", "예약이 취소되었습니다."),
    RESERVATION_CHECKOUT_PHOTO(HttpStatus.OK, "RESERVATION200_6", "퇴실 증빙이 제출되었습니다."),
    RESERVATION_CHECKOUT_OK(HttpStatus.OK, "RESERVATION200_7", "퇴실이 승인되어 정산이 완료되었습니다."),
    RESERVATION_REQUEST(HttpStatus.CREATED, "RESERVATION201_1", "예약 요청이 완료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
