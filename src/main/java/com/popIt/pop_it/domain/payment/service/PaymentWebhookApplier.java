package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import java.util.function.Consumer;
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
    public void apply(Long paymentId, PaymentResDTO.TossConfirm actual) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("웹훅 반영 대상 결제를 찾을 수 없음: paymentId={}", paymentId);
            return;
        }

        switch (actual.status()) {
            case "DONE" -> markPaidIfNotAlready(payment, actual);
            case "EXPIRED" -> markIfPending(payment, Payment::markAsExpired, "만료");
            case "ABORTED" -> markIfPending(payment, Payment::markAsFailed, "실패");
            default -> log.info("반영하지 않는 결제 상태: paymentId={}, status={}",
                    payment.getId(), actual.status());
        }
    }

    private void markPaidIfNotAlready(Payment payment, PaymentResDTO.TossConfirm actual) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        payment.markAsPaid(
                actual.paymentKey(),
                PaymentMethod.fromDescription(actual.method()),
                actual.approvedAt().toLocalDateTime()
        );
        // 계약 완료 처리
        payment.getContract().markAsCompleted();
        log.info("웹훅으로 결제 완료 반영: paymentId={}", payment.getId());
    }

    private void markIfPending(Payment payment, Consumer<Payment> mutation, String label) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        mutation.accept(payment);
        log.info("웹훅으로 결제 {} 반영: paymentId={}", label, payment.getId());
    }
}
