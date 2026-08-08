package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import java.util.function.ToIntFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// PaymentWebhookService.handle()이 토스 조회로 검증을 마친 뒤에만 호출한다.
// 검증(외부 API 호출)은 트랜잭션 밖에서 이루어지므로, 실제 DB 반영은 이 짧은 트랜잭션 안에서
// Payment를 새로 조회해 최신 상태 기준으로 처리한다.
@Slf4j
@Component
@RequiredArgsConstructor
class PaymentWebhookApplier {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void apply(Long paymentId, PaymentResDTO.TossConfirmRes actual) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("웹훅 반영 대상 결제를 찾을 수 없음: paymentId={}", paymentId);
            return;
        }

        switch (actual.status()) {
            case "DONE" -> markPaidIfNotAlready(payment, actual);
            case "EXPIRED" -> markIfPending(payment.getId(), paymentRepository::markExpiredIfPending, "만료");
            case "ABORTED" -> markIfPending(payment.getId(), paymentRepository::markFailedIfPending, "실패");
            default -> log.info("반영하지 않는 결제 상태: paymentId={}, status={}",
                    payment.getId(), actual.status());
        }
    }

    // EXPIRED/ABORTED와 달리 PENDING 조건으로 좁히지 않고 PAID가 아니면 반영한다.
    private void markPaidIfNotAlready(Payment payment, PaymentResDTO.TossConfirmRes actual) {
        boolean alreadyPaid = payment.getStatus() == PaymentStatus.PAID;
        if (!alreadyPaid) {
            payment.markAsPaid(
                    actual.paymentKey(),
                    PaymentMethod.fromDescription(actual.method()),
                    actual.approvedAt().toLocalDateTime()
            );
        }

        Contract contract = payment.getContract();
        if (!alreadyPaid && contract.getStatus() == ContractStatus.COMPLETED) {
            // 같은 계약에 토스 승인이 두 번 들어온 이중 청구 상황.
            // 결제 자체는 PAID로 남기고 환불 등 운영 처리가 필요함을 ERROR로 알린다.
            log.error("계약당 결제 중복 승인 감지(이중 청구 의심, 환불 필요): paymentId={}, contractId={}, amount={}",
                payment.getId(), contract.getId(), actual.totalAmount());
            return;
        }

        completeContractIfNeeded(payment);
        log.info("웹훅으로 결제 완료 반영: paymentId={}", payment.getId());
    }

    // 이미 완료된 계약이면 아무것도 하지 않는 멱등 연산
    private void completeContractIfNeeded(Payment payment) {
        Contract contract = payment.getContract();
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            return;
        }
        contract.markAsCompleted();
        contract.getReservation().markPaymentCompleted();
    }

    // TOCTOU(check-then-act) 취약점을 방어하기 위해 조건과 반영을 조건부 UPDATE로 한 번에 처리해,
    // 커밋 시점에 실제로 아직 PENDING인 경우에만 반영되게 한다.
    private void markIfPending(Long paymentId, ToIntFunction<Long> conditionalUpdate, String label) {
        if (conditionalUpdate.applyAsInt(paymentId) > 0) {
            log.info("웹훅으로 결제 {} 반영: paymentId={}", label, paymentId);
        }
    }
}
