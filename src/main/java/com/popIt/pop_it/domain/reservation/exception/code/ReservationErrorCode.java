package com.popIt.pop_it.domain.reservation.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReservationErrorCode implements BaseErrorCode {

    // ===== 400 BAD REQUEST : 요청 자체가 잘못된 경우 =====
    RESERVATION_INVALID_DATE(HttpStatus.BAD_REQUEST, "RESERVATION400_1", "예약 가능한 날짜/시간이 아닙니다."),
    RESERVATION_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "RESERVATION400_2", "예약 시작일이 종료일보다 늦을 수 없습니다."),
    RESERVATION_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "RESERVATION400_3", "결제 금액이 예약 금액과 일치하지 않습니다."),
    RESERVATION_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "RESERVATION400_4", "취소 또는 완료된 예약은 수정할 수 없습니다."),
    RESERVATION_HOLD_EXPIRED(HttpStatus.BAD_REQUEST, "RESERVATION400_5", "예약 대기(hold) 시간이 만료되어 결제를 진행할 수 없습니다."),

    // ===== 403 FORBIDDEN : 권한 없음 =====
    RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RESERVATION403_1", "본인의 예약이 아니므로 접근할 수 없습니다."),
    RESERVATION_HOST_PAYMENT_DENIED(HttpStatus.FORBIDDEN, "RESERVATION403_2", "호스트는 게스트의 예약을 직접 결제 처리할 수 없습니다."),

    // ===== 404 NOT FOUND : 리소스 없음 =====
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION404_1", "해당 예약을 찾을 수 없습니다."),
    RESERVATION_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION404_2", "해당 예약에 연결된 결제 정보를 찾을 수 없습니다."),

    // ===== 409 CONFLICT : 동시성/중복/상태 충돌 =====
    RESERVATION_ALREADY_TAKEN(HttpStatus.CONFLICT, "RESERVATION409_1", "이미 예약이 완료된 공간입니다. (동시성 충돌)"),
    RESERVATION_PAYMENT_IN_PROGRESS(HttpStatus.CONFLICT, "RESERVATION409_2", "다른 사용자가 결제를 진행 중입니다. 잠시 후 다시 시도해주세요."),
    RESERVATION_PAYMENT_DUPLICATED(HttpStatus.CONFLICT, "RESERVATION409_3", "이미 처리된 결제입니다. (중복 결제 방지)"),
    RESERVATION_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "RESERVATION409_4", "현재 예약 상태에서는 취소/환불이 불가능합니다."),

    // ===== 500 INTERNAL SERVER ERROR : 서버/외부 PG 연동 오류 =====
    RESERVATION_PAYMENT_APPROVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RESERVATION500_1", "결제 승인 처리 중 오류가 발생했습니다."),
    RESERVATION_PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RESERVATION500_2", "결제 취소(환불) 처리 중 오류가 발생했습니다."),
    RESERVATION_ESCROW_SETTLEMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RESERVATION500_3", "에스크로 정산 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
