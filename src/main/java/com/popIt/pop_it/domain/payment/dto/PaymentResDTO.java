package com.popIt.pop_it.domain.payment.dto;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.payment.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.Builder;

public class PaymentResDTO {

    @Builder
    public record Prepare(
            @Schema(description = "결제 식별자", example = "1")
            Long paymentId,

            @Schema(description = "토스페이먼츠에 전달하는 주문번호", example = "ORDER_1_a1b2c3d4e5f6")
            String orderId,

            @Schema(description = "주문명 (공간 건물명)", example = "팝잇 빌딩")
            String orderName,

            @Schema(description = "총 결제 금액 (임대료 + 보증금 + 보험료)", example = "155000")
            Long amount,

            @Schema(description = "임대료", example = "100000")
            Long rentFee,

            @Schema(description = "보증금", example = "50000")
            Long deposit,

            @Schema(description = "보험료", example = "5000")
            Long insuranceFee,

            @Schema(description = "결제 상태", example = "PENDING")
            String status
    ) {
        public static Prepare of(Payment payment, Contract contract) {
            return Prepare.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .orderName(contract.getReservation().getSpace().getBuildingName())
                    .amount(contract.getTotalPrice())
                    .rentFee(contract.getRentalFee())
                    .deposit(contract.getDeposit())
                    .insuranceFee(contract.getInsuranceFee())
                    .status(payment.getStatus().name())
                    .build();
        }
    }

    @Builder
    public record Confirm(
            @Schema(description = "결제 식별자", example = "1")
            Long paymentId,

            @Schema(description = "토스페이먼츠에 전달한 주문번호", example = "ORDER_1_a1b2c3d4e5f6")
            String orderId,

            @Schema(description = "결제 수단", example = "CARD")
            String method,

            @Schema(description = "결제 상태", example = "PAID")
            String status,

            @Schema(description = "결제 승인 일시", example = "2026-07-16T13:45:00")
            LocalDateTime paidAt
    ) {
        public static Confirm of(Payment payment) {
            return Confirm.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                    .status(payment.getStatus().name())
                    .paidAt(payment.getPaidAt())
                    .build();
        }
    }

    public record TossConfirm(
            String paymentKey,
            String orderId,
            String method, // 카드, 간편결제, 휴대폰, 계좌이체, 문화상품권, 도서문화상품권, 게임문화상품권 (가상계좌 사용X)
            String status, // READY, DONE, CANCELED, PARTIAL_CANCELED 등
            Long totalAmount,
            OffsetDateTime approvedAt
    ) {
    }

    // 토스페이먼츠 API 실패 응답 바디 (예: ALREADY_PROCESSED_PAYMENT, INVALID_CARD_NUMBER 등)
    public record TossError(
            String code,
            String message
    ) {
    }
}
