package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.exception.ContractErrorCode;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.payment.client.HostPayoutClient;
import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.converter.PaymentConverter;
import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.enums.SettlementStepStatus;
import com.popIt.pop_it.domain.payment.exception.PaymentErrorCode;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final PaymentIdempotentSaver paymentIdempotentSaver;
    private final PaymentSettlementRecorder paymentSettlementRecorder;
    private final TossPaymentClient tossPaymentClient;
    private final HostPayoutClient hostPayoutClient;

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
    public PaymentResDTO.Confirm confirm(Long paymentId, PaymentReqDTO.Confirm reqDTO, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 본인 계약의 결제인지 확인
        if (!payment.getContract().getReservation().getUser().getUserId().equals(userId)) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }

        // 이미 승인된 결제면 재승인을 시도하지 않고 그대로 반환한다.
        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentResDTO.Confirm.of(payment);
        }

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
            // 계약 완료 처리
            payment.getContract().markAsCompleted();
        } catch (ProjectException e) {
            payment.markAsFailed();
            throw e;
        }

        return PaymentResDTO.Confirm.of(payment);
    }

    /**
     * 퇴실 승인 시 내부적으로 호출되는 정산 처리.
     * (1) 임대료를 호스트에게 지급 (2) 보증금을 게스트에게 부분취소(환불)
     * <p>
     * 둘 다 신뢰할 수 없는 외부 API 호출이라 하나가 실패해도 다른 하나의 시도를 막지 않는다.
     * 각 단계의 성공/실패는 별도 트랜잭션(PaymentSettlementRecorder)에 즉시 커밋되므로,
     * 이 메서드가 최종적으로 예외를 던져도 이미 완료된 단계의 기록은 롤백되지 않는다.
     * 이미 DONE인 단계는 건너뛰므로, 실패했던 단계만 골라 안전하게 재시도할 수 있다.
     */
    // @TODO: 퇴실 사진 승인 시 호출
    // @TODO: 정산 실패 단계 재시도 필요
    @Transactional(readOnly = true)
    public void settle(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_NOT_PAID);
        }

        Contract contract = payment.getContract();

        boolean hostPayoutOk = settleHostPayout(payment, contract);
        boolean depositRefundOk = settleDepositRefund(payment, contract);

        if (!hostPayoutOk || !depositRefundOk) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_SETTLEMENT_FAILED);
        }
    }

    private boolean settleHostPayout(Payment payment, Contract contract) {
        if (payment.getHostPayoutStatus() == SettlementStepStatus.DONE) {
            return true;
        }
        try {
            Long hostId = contract.getReservation().getSpace().getHostId();
            hostPayoutClient.payout(payment.getOrderId() + "-HOST", hostId, contract.getRentalFee());
            paymentSettlementRecorder.update(payment.getId(), Payment::markHostPayoutDone);
            return true;
        } catch (Exception e) {
            log.warn("호스트 정산 지급 실패: paymentId={}", payment.getId(), e);
            markFailedBestEffort(payment.getId(), Payment::markHostPayoutFailed);
            return false;
        }
    }

    private boolean settleDepositRefund(Payment payment, Contract contract) {
        if (payment.getDepositRefundStatus() == SettlementStepStatus.DONE) {
            return true;
        }
        try {
            tossPaymentClient.cancelPartial(
                    payment.getPaymentKey(), contract.getDeposit(), "퇴실 승인에 따른 보증금 환불");
            paymentSettlementRecorder.update(payment.getId(), Payment::markDepositRefundDone);
            return true;
        } catch (Exception e) {
            log.warn("보증금 환불 실패: paymentId={}", payment.getId(), e);
            markFailedBestEffort(payment.getId(), Payment::markDepositRefundFailed);
            return false;
        }
    }

    // 실패 기록 자체가 실패해도(예: REQUIRES_NEW 커밋 중 일시적 오류) settle()을 중단시키지 않는다.
    private void markFailedBestEffort(Long paymentId, Consumer<Payment> mutation) {
        try {
            paymentSettlementRecorder.update(paymentId, mutation);
        } catch (Exception e) {
            log.warn("정산 실패 상태 기록도 실패: paymentId={}", paymentId, e);
        }
    }
}
