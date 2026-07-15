package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.exception.ContractErrorCode;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.converter.PaymentConverter;
import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.exception.PaymentErrorCode;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final PaymentIdempotentSaver paymentIdempotentSaver;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public PaymentResDTO.Prepare prepare(Long contractId, String idempotencyKey, Long userId) {

        // 멱등
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();

            // 만약 같은 멱등키로 다른 계약이 들어왔으면 에러
            if (!payment.getContract().getId().equals(contractId)) {
                throw new ProjectException(PaymentErrorCode.PAYMENT_CONFLICT_KEY);
            }

            switch (payment.getStatus()) {
                case PAID -> throw new ProjectException(PaymentErrorCode.PAYMENT_ALREADY_PAID);
                case FAILED, EXPIRED -> throw new ProjectException(PaymentErrorCode.PAYMENT_RETRYABLE); // 새 키로 재시도 필요
                default -> {
                    return PaymentResDTO.Prepare.of(payment, payment.getContract());
                }
            }
        }

        // 계약 검증
        Contract contract = contractRepository.findWithReservationAndUserById(contractId)
                .orElseThrow(() -> new ProjectException(ContractErrorCode.CONTRACT_NOT_FOUND));

        // 본인인지 확인
        if (!contract.getReservation().getUser().getUserId().equals(userId)) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }

        // 결제 가능한 상태인지 확인
        if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_CONFLICT_PAYMENT);
        }

        // orderId 생성
        String orderId = generateOrderId(contractId);

        // PENDING 저장 (동시 요청 레이스는 별도 트랜잭션에서 UNIQUE 제약으로 최종 방어)
        Payment payment = PaymentConverter.toPayment(contract, orderId, idempotencyKey);
        Payment savedPayment = paymentIdempotentSaver.save(payment, idempotencyKey);

        return PaymentResDTO.Prepare.of(savedPayment, contract);
    }

    private String generateOrderId(Long contractId) {
        return "ORDER_" + contractId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Transactional
    public PaymentResDTO.Confirm confirm(Long paymentId, PaymentReqDTO.Confirm reqDTO) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getOrderId().equals(reqDTO.orderId())) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_ORDER_MISMATCH);
        }

        if (!payment.getContract().getTotalPrice().equals(reqDTO.amount())) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        try {
            PaymentResDTO.TossConfirm tossConfirm =
                    tossPaymentClient.confirm(reqDTO.paymentKey(), reqDTO.orderId(), reqDTO.amount());

            payment.markAsPaid(
                    tossConfirm.paymentKey(),
                    PaymentMethod.fromDescription(tossConfirm.method()),
                    tossConfirm.approvedAt().toLocalDateTime()
            );
        } catch (ProjectException e) {
            payment.markAsFailed();
            throw e;
        }

        return PaymentResDTO.Confirm.of(payment);
    }
}
