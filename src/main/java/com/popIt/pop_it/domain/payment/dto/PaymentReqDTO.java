package com.popIt.pop_it.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class PaymentReqDTO {

    public record Confirm(
            String paymentKey,
            String orderId,
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
