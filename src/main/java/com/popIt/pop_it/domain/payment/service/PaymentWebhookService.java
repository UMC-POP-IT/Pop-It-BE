package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public void handle(PaymentReqDTO.Webhook payload) {
        if (!PAYMENT_STATUS_CHANGED.equals(payload.eventType())) {
            log.info("처리 대상이 아닌 웹훅 이벤트: {}", payload.eventType());
            return;
        }

        PaymentReqDTO.Webhook.WebhookData data = payload.data();
        if (data == null || data.orderId() == null || data.paymentKey() == null) {
            log.warn("웹훅 payload에 data가 없거나 불완전함: {}", payload);
            return;
        }

        Payment payment = paymentRepository.findByOrderId(data.orderId()).orElse(null);
        if (payment == null) {
            log.warn("웹훅 대상 결제를 찾을 수 없음: orderId={}", data.orderId());
            return;
        }

        // webhook payload는 서명 검증이 없어 위변조될 수 있으므로, 조회 API로 실제 상태를 재확인한 뒤에만 반영한다.
        PaymentResDTO.TossConfirm actual = tossPaymentClient.getPayment(data.paymentKey());
        if (!actual.orderId().equals(payment.getOrderId())) {
            log.warn("웹훅 조회 결과의 주문번호 불일치: expected={}, actual={}",
                    payment.getOrderId(), actual.orderId());
            return;
        }

        applyStatus(payment, actual);
    }

    private void applyStatus(Payment payment, PaymentResDTO.TossConfirm actual) {
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
