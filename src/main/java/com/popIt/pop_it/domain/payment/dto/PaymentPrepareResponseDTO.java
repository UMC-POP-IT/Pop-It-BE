package com.popIt.pop_it.domain.payment.dto;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.payment.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentPrepareResponseDTO(
        Long paymentId,
        String orderId,
        String orderName,
        Long amount,
        Long rentFee,
        Long deposit,
        Long insuranceFee,
        String status) {
    public static PaymentPrepareResponseDTO of(Payment payment, Contract contract) {
        return new PaymentPrepareResponseDTO(
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
