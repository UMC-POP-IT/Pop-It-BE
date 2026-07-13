package com.popIt.pop_it.domain.payment.dto;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.payment.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class PaymentResDTO {

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
            return new Prepare(
                    payment.getId(),
                    payment.getOrderId(),
                    contract.getReservation().getSpace().getBuildingName(),
                    contract.getTotalPrice(),
                    contract.getRentalFee(),
                    contract.getDeposit(),
                    contract.getInsuranceFee(),
                    payment.getStatus().name()
            );
        }
    }

    public record Confirm(
            Long paymentId,
            String orderId,
            String method,
            String status,
            LocalDateTime paidAt
    ) {
        public static Confirm of(Payment payment) {
            return new Confirm(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getMethod() != null ? payment.getMethod().name() : null,
                    payment.getStatus().name(),
                    payment.getPaidAt()
            );
        }
    }
}
