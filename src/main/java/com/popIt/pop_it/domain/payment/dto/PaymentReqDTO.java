package com.popIt.pop_it.domain.payment.dto;

public class PaymentReqDTO {

    public record Confirm(
            String paymentKey,
            String orderId,
            Long amount
    ) {
    }
}
