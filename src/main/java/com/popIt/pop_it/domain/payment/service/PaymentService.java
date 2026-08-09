package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.exception.code.ContractErrorCode;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.contract.service.ContractService;
import com.popIt.pop_it.domain.payment.client.HostPayoutClient;
import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.converter.PaymentConverter;
import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.enums.SettlementStepStatus;
import com.popIt.pop_it.domain.payment.exception.code.PaymentErrorCode;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final ContractService contractService;
    private final PaymentIdempotentSaver paymentIdempotentSaver;
    private final PaymentSettlementRecorder paymentSettlementRecorder;
    private final ContractCompletionService contractCompletionService;
    private final TossPaymentClient tossPaymentClient;
    private final HostPayoutClient hostPayoutClient;

    @Transactional
    public PaymentResDTO.PaymentPrepareRes prepare(Long contractId, String idempotencyKey, Long userId) {

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
                    return PaymentResDTO.PaymentPrepareRes.of(payment, payment.getContract());
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

        // 결제 전, 계약 체결 이후 내용이 변조되지 않았는지 검증
        contractService.verifyContentIntegrity(contract);

        // 결제 가능한 상태인지 확인
        if (contract.getStatus() != ContractStatus.PENDING_PAYMENT) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_CONFLICT_PAYMENT);
        }

        // orderId 생성
        String orderId = generateOrderId(contractId);

        // PENDING 저장. 동시 요청 레이스는 별도 트랜잭션에서 DB 제약으로 최종 방어한다:
        // - idempotencyKey unique 제약: 같은 Idempotency-Key로 동시 요청이 들어온 경우
        // - payment(contract_id) partial unique index (status='PENDING'): 서로 다른
        //   Idempotency-Key라도 같은 계약에 PENDING 결제가 이미 있으면 새로 만들지 못하게 막는다
        Payment payment = PaymentConverter.toPayment(contract, orderId, idempotencyKey);
        Payment savedPayment;
        try {
            savedPayment = paymentIdempotentSaver.save(payment);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 먼저 저장에 성공한 경우: 실패한 저장 트랜잭션과 독립된 이 트랜잭션
            // (정상 상태)에서 재조회해 그 결제를 반환한다. 어떤 제약을 위반했는지는 구분하지 않고
            // Idempotency-Key로 먼저 찾아보고, 없으면 같은 계약의 PENDING 결제를 찾는다.
            savedPayment = paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> {
                        // 같은 멱등키로 다른 계약의 결제가 먼저 저장된 경우: 그 결제를 성공 결과로 반환하지 않는다.
                        if (!existing.getContract().getId().equals(contractId)) {
                            throw new ProjectException(PaymentErrorCode.PAYMENT_CONFLICT_KEY);
                        }
                        return existing;
                    })
                    .or(() -> paymentRepository.findByContractIdAndStatus(contractId, PaymentStatus.PENDING))
                    .orElseThrow(() -> e);
        }

        return PaymentResDTO.PaymentPrepareRes.of(savedPayment, contract);
    }

    private String generateOrderId(Long contractId) {
        return "ORDER_" + contractId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Transactional
    public PaymentResDTO.PaymentConfirmRes confirm(Long paymentId, PaymentReqDTO.PaymentConfirmReq reqDTO, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 본인 계약의 결제인지 확인
        if (!payment.getContract().getReservation().getUser().getUserId().equals(userId)) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }

        // 이미 승인된 결제면 토스 재승인 없이, 직전 시도에서 못 끝냈을 계약/예약 완료만 이어서 마무리한다.
        if (payment.getStatus() == PaymentStatus.PAID) {
            completeContractSafely(payment, reqDTO, true);
            return PaymentResDTO.PaymentConfirmRes.of(payment);
        }

        // 실패/만료된 결제는 재승인 대상이 아니다 - prepare()에서 새 Idempotency-Key로 다시 준비해야 한다.
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.EXPIRED) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_RETRYABLE);
        }

        if (!payment.getOrderId().equals(reqDTO.orderId())) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_ORDER_MISMATCH);
        }

        if (!payment.getContract().getTotalPrice().equals(reqDTO.amount())) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PaymentResDTO.TossConfirmRes tossConfirm;
        try {
            tossConfirm = confirmWithToss(reqDTO, payment.getId());
        } catch (ProjectException e) {
            if (e.getErrorCode() != PaymentErrorCode.PAYMENT_CONFIRM_RECONCILIATION_PENDING) {
                paymentSettlementRecorder.markConfirmFailedIfPending(payment.getId());
            }
            throw e;
        }

        // 토스 승인은 이미 확정됐으니, 계약/예약 완료 처리와 별개로 이 사실부터 별도
        // 트랜잭션으로 durable하게 먼저 커밋한다 - 뒤 단계가 실패해도 이 커밋은 롤백되지 않는다.
        paymentSettlementRecorder.update(payment.getId(), p -> p.markAsPaid(
                tossConfirm.paymentKey(),
                PaymentMethod.fromDescription(tossConfirm.method()),
                tossConfirm.approvedAt().toLocalDateTime()));
        // 응답/이후 처리에 최신 PAID 상태를 쓰려면 같은 객체를 직접 갱신해야 한다.
        payment.markAsPaid(
                tossConfirm.paymentKey(),
                PaymentMethod.fromDescription(tossConfirm.method()),
                tossConfirm.approvedAt().toLocalDateTime());
        completeContractSafely(payment, reqDTO, false);

        return PaymentResDTO.PaymentConfirmRes.of(payment);
    }

    // 계약/예약 완료 처리를 시도하고, 실패해도 결제(PAID)는 이미 확정돼 있으니 FAILED로 되돌리지 않는다.
    private void completeContractSafely(Payment payment, PaymentReqDTO.PaymentConfirmReq reqDTO, boolean alreadyPaid) {
        try {
            contractCompletionService.completeIfNeeded(payment, alreadyPaid);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 다른 경로(웹훅 등)가 같은 계약을 먼저 완료 처리해 버전이 충돌한 경우.
            // 이 결제 자체는 이미 성공했으므로 실패로 기록하지 않고, 재조회를 안내한다.
            log.warn("계약 버전 충돌로 완료 처리 커밋 실패(다른 경로가 먼저 처리한 것으로 추정): paymentId={}",
                    payment.getId(), e);
            throw new ProjectException(PaymentErrorCode.PAYMENT_CONCURRENT_MODIFICATION);
        } catch (Exception e) {
            log.error("결제 PAID 반영 후 계약 완료 처리 실패: paymentId={}, orderId={}",
                    payment.getId(), reqDTO.orderId(), e);
            throw new ProjectException(PaymentErrorCode.PAYMENT_CONFIRM_RECONCILIATION_PENDING);
        }
    }

    // 응답을 못 받은 경우(PAYMENT_GATEWAY_UNAVAILABLE)는 토스가 실제로는 승인했을 수 있어
    // 재조회로 확인 후에만 실패 처리한다 (이중 청구 방지)
    private PaymentResDTO.TossConfirmRes confirmWithToss(PaymentReqDTO.PaymentConfirmReq reqDTO, Long paymentId) {
        try {
            return tossPaymentClient.confirm(reqDTO.paymentKey(), reqDTO.orderId(), reqDTO.amount());
        } catch (ProjectException e) {
            if (e.getErrorCode() != PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE) {
                throw e;
            }
            PaymentResDTO.TossConfirmRes actual;
            try {
                actual = tossPaymentClient.getPayment(reqDTO.paymentKey());
            } catch (ProjectException verifyFailure) {
                // 토스의 실제 승인 여부를 전혀 확인할 수 없다.
                // FAILED로 단정해 새 결제 생성을 허용하면 이중 청구 위험이 있으므로 예외를 던진다.
                log.error("토스 승인 확인 불가(원 요청/재조회 모두 실패): paymentId={}", paymentId, verifyFailure);
                throw new ProjectException(PaymentErrorCode.PAYMENT_CONFIRM_RECONCILIATION_PENDING);
            }
            if (!"DONE".equals(actual.status())
                    || !actual.orderId().equals(reqDTO.orderId())
                    || !actual.totalAmount().equals(reqDTO.amount())) {
                throw new ProjectException(PaymentErrorCode.PAYMENT_RETRYABLE);
            }
            log.warn("confirm 응답 유실 후 재조회로 실제 승인 확인됨(이중 결제 방지): paymentId={}", paymentId);
            return actual;
        }
    }

    // 퇴실 승인 시 예약 ID로 결제를 찾아 정산한다.
    // REQUIRES_NEW: 호출하는 쪽(퇴실 승인)의 트랜잭션과 분리해, 정산 중의 느린 외부 API 호출이
    // 호출자의 DB 커넥션/락을 붙잡고 있지 않게 하고, 두 트랜잭션의 커밋 성패가 서로 얽히지 않게 한다.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void settleByReservation(Long reservationId) {
        Payment payment = paymentRepository.findByContractReservationIdAndStatus(reservationId, PaymentStatus.PAID)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        settle(payment.getId());
    }

    // 퇴실이 정상 승인되지 않은 경로(증빙 미제출 타임아웃, 거절 후 재제출 없이 타임아웃)에서 호출된다.
    // 이 경우 보증금 환불은 시도하지 않고 호스트 정산만 진행한다.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void settleHostPayoutOnlyByReservation(Long reservationId) {
        Payment payment = paymentRepository.findByContractReservationIdAndStatus(reservationId, PaymentStatus.PAID)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        settleHostPayoutOnly(payment.getId());
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

    // 보증금 환불 단계는 건드리지 않고 SKIPPED로 고정한 채 호스트 정산만 진행한다.
    // SKIPPED로 표시해두면 이후 재시도 스케줄러가 FAILED 단계만 골라 재시도할 때도 이 결제의
    // 보증금 환불은 건드리지 않는다.
    @Transactional(readOnly = true)
    public void settleHostPayoutOnly(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_NOT_PAID);
        }

        // PENDING일 때만 SKIPPED로 원자적(조건부 UPDATE)으로 전환한다.
        paymentSettlementRecorder.skipDepositRefundIfPending(paymentId);

        if (!settleHostPayout(payment, payment.getContract())) {
            throw new ProjectException(PaymentErrorCode.PAYMENT_SETTLEMENT_FAILED);
        }
    }

    private boolean settleHostPayout(Payment payment, Contract contract) {
        if (payment.getHostPayoutStatus() == SettlementStepStatus.DONE) {
            return true;
        }
        // 외부 호출 전에 DB에서 PROCESSING으로 선점한다. 동시에 두 실행이 여기 도달해도
        // 조건부 UPDATE가 행 잠금을 거쳐 순차 처리되므로 하나만 선점에 성공한다.
        if (!paymentSettlementRecorder.claimHostPayout(payment.getId())) {
            // 이미 다른 실행이 처리 중이거나(PROCESSING) 그 사이 완료된 경우: 중복 호출하지 않는다.
            return isHostPayoutDone(payment.getId());
        }
        try {
            Long hostId = contract.getReservation().getSpace().getHostId();
            Long hostPayoutAmount = contract.getHostTotalPrice();
            hostPayoutClient.payout(payment.getOrderId() + "-HOST", hostId, hostPayoutAmount);
            paymentSettlementRecorder.update(payment.getId(), Payment::markHostPayoutDone);
            return true;
        } catch (Exception e) {
            log.warn("호스트 정산 지급 실패: paymentId={}", payment.getId(), e);
            markFailedBestEffort(payment.getId(), Payment::markHostPayoutFailed);
            return false;
        }
    }

    private boolean settleDepositRefund(Payment payment, Contract contract) {
        if (payment.getDepositRefundStatus() == SettlementStepStatus.DONE
                || payment.getDepositRefundStatus() == SettlementStepStatus.SKIPPED) {
            return true;
        }
        if (!paymentSettlementRecorder.claimDepositRefund(payment.getId())) {
            return isDepositRefundDone(payment.getId());
        }
        try {
            // 취소는 성공했지만 아래 기록이 실패해 재시도되는 경우를 대비해, orderId 기반의
            // 고정된 키를 매번 동일하게 전달한다 (토스가 같은 키의 재요청을 중복 취소로 처리하지 않도록).
            tossPaymentClient.cancelPartial(
                    payment.getPaymentKey(), contract.getDeposit(), "퇴실 승인에 따른 보증금 환불",
                    payment.getOrderId() + "-REFUND");
            paymentSettlementRecorder.update(payment.getId(), Payment::markDepositRefundDone);
            return true;
        } catch (Exception e) {
            log.warn("보증금 환불 실패: paymentId={}", payment.getId(), e);
            markFailedBestEffort(payment.getId(), Payment::markDepositRefundFailed);
            return false;
        }
    }

    private boolean isHostPayoutDone(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(p -> p.getHostPayoutStatus() == SettlementStepStatus.DONE)
                .orElse(false);
    }

    private boolean isDepositRefundDone(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(p -> p.getDepositRefundStatus() == SettlementStepStatus.DONE
                        || p.getDepositRefundStatus() == SettlementStepStatus.SKIPPED)
                .orElse(false);
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
