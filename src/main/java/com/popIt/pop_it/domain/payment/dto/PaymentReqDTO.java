package com.popIt.pop_it.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PaymentReqDTO {

    public record Confirm(
            @NotBlank(message = "paymentKey는 필수입니다.")
            String paymentKey,

            @NotBlank(message = "orderId는 필수입니다.")
            String orderId,

            @NotNull(message = "amount는 필수입니다.")
            @Positive(message = "amount는 양수여야 합니다.")
            Long amount
    ) {
    }

    // 토스페이먼츠 웹훅 payload
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Webhook(
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
