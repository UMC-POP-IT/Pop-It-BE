package com.popIt.pop_it.domain.payment.converter;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;

public class PaymentConverter {

    // 결제 준비
    public static Payment toPayment(Contract contract, String orderId, String idempotencyKey) {
        return Payment.builder()
                .contract(contract)
                .orderId(orderId)
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.PENDING)
                .build();
    }
}
