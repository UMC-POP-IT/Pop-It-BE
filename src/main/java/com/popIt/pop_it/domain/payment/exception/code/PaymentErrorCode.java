package com.popIt.pop_it.domain.payment.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements BaseErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT404_1", "결제를 찾을 수 없습니다."),
    PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "PAYMENT403_1", "본인의 계약만 결제할 수 있습니다."),
    PAYMENT_CONFLICT_PAYMENT(HttpStatus.CONFLICT, "PAYMENT409_1", "결제 가능한 계약 상태가 아닙니다."),
    PAYMENT_CONFLICT_KEY(HttpStatus.CONFLICT, "PAYMENT409_2", "동일한 Idempotency-Key로 다른 요청이 이미 처리되었습니다."),
    PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "PAYMENT409_3", "이미 결제가 완료되었습니다."),
    PAYMENT_RETRYABLE(HttpStatus.CONFLICT, "PAYMENT409_4", "이전 결제 시도가 실패했습니다. 새로운 Idempotency-Key로 다시 요청해주세요."),
    PAYMENT_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT400_1", "요청한 주문번호가 결제 정보와 일치하지 않습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT400_2", "요청한 결제 금액이 계약 금액과 일치하지 않습니다."),
    PAYMENT_NOT_PAID(HttpStatus.CONFLICT, "PAYMENT409_5", "결제가 완료되지 않아 정산할 수 없습니다."),
    PAYMENT_SETTLEMENT_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT502_1", "정산 처리 중 일부가 실패했습니다. 잠시 후 재시도해주세요."),
    PAYMENT_GATEWAY_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "PAYMENT502_2", "결제 게이트웨이와 통신할 수 없습니다. 잠시 후 다시 시도해주세요."),
    PAYMENT_METHOD_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "PAYMENT400_3", "지원하지 않는 결제 수단입니다."),
    PAYMENT_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "PAYMENT409_6", "계약 처리가 동시에 진행되었습니다. 잠시 후 다시 조회해주세요."),
    PAYMENT_CONFIRM_RECONCILIATION_PENDING(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT500_1", "결제 확인 처리 중 오류가 발생했습니다. 새로 결제하지 마시고, 같은 결제 승인 요청을 잠시 후 다시 보내주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
