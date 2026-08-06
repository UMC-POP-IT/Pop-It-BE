package com.popIt.pop_it.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PaymentReqDTO {

    public record PaymentConfirmReq(
            @Schema(description = "토스페이먼츠 결제창 인증 완료 후 전달받은 결제 키", example = "5EnNZRJGvaBX7zk2yd5RY")
            @NotBlank(message = "paymentKey는 필수입니다.")
            String paymentKey,

            @Schema(description = "결제 요청(prepare) 시 발급받은 주문 ID", example = "order-a1b2c3d4")
            @NotBlank(message = "orderId는 필수입니다.")
            String orderId,

            @Schema(description = "결제 금액. prepare 시 계산된 총 결제 금액과 일치해야 함", example = "522500")
            @NotNull(message = "amount는 필수입니다.")
            @Positive(message = "amount는 양수여야 합니다.")
            Long amount
    ) {
    }

    // 토스페이먼츠 웹훅 payload
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentWebhookReq(
            String eventType, // 예: PAYMENT_STATUS_CHANGED
            WebhookData data
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record WebhookData(
                String paymentKey,
                String orderId,
                String status // DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED 등
        ) {
        }
    }
}
