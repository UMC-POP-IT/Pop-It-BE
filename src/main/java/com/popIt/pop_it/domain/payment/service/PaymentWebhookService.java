package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentWebhookApplier paymentWebhookApplier;

    // 검증(payload 확인, 결제 조회, 토스 API 호출)은 트랜잭션 밖에서 수행한다.
    public void handle(PaymentReqDTO.Webhook payload) {
        if (!PAYMENT_STATUS_CHANGED.equals(payload.eventType())) {
            log.info("처리 대상이 아닌 웹훅 이벤트: {}", payload.eventType());
            return;
        }

        PaymentReqDTO.Webhook.WebhookData data = payload.data();
        if (data == null || data.orderId() == null || data.paymentKey() == null) {
            log.warn("웹훅 payload에 data가 없거나 불완전함: eventType={}", payload.eventType());
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

        paymentWebhookApplier.apply(payment.getId(), actual);
    }
}
