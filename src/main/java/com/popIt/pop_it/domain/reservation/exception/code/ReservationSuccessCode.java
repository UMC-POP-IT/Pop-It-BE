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
    RESERVATION_HOST_CANCEL(HttpStatus.OK, "RESERVATION200_5", "예약이 취소되었습니다."), //삭제 예정
    RESERVATION_CHECKOUT_PHOTO(HttpStatus.OK, "RESERVATION200_6", "퇴실 증빙이 제출되었습니다."),
    RESERVATION_CHECKOUT_OK(HttpStatus.OK, "RESERVATION200_7", "퇴실이 승인되어 정산이 완료되었습니다."),
    RESERVATION_UNAVAILABLE_DATES(HttpStatus.OK, "RESERVATION200_8", "예약 불가 날짜가 조회되었습니다."),
    RESERVATION_CHECKOUT_IMAGES(HttpStatus.OK, "RESERVATION200_9", "퇴실 증빙 사진이 조회되었습니다."),
    RESERVATION_CHECKOUT_REJECT(HttpStatus.OK, "RESERVATION200_10", "퇴실 증빙을 거절했습니다."),
    RESERVATION_CHECKOUT_IMAGES_ME(HttpStatus.OK, "RESERVATION200_11", "퇴실 증빙 사진이 조회되었습니다."),
    RESERVATION_CHECKOUT_APPROVAL(HttpStatus.OK, "RESERVATION200_12", "퇴실 승인 여부가 조회되었습니다."),
    RESERVATION_REQUEST(HttpStatus.CREATED, "RESERVATION201_1", "예약 요청이 완료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
