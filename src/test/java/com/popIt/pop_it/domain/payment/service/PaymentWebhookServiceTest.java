package com.popIt.pop_it.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @InjectMocks
    private PaymentWebhookService paymentWebhookService;

    private static final String ORDER_ID = "ORDER_1_abcdefabcdef";
    private static final String PAYMENT_KEY = "payment-key-1";

    private Payment paymentOf(PaymentStatus status) {
        return Payment.builder()
                .id(1L)
                .status(status)
                .orderId(ORDER_ID)
                .idempotencyKey("idem-key-1")
                .build();
    }

    private PaymentReqDTO.Webhook webhookOf(String status) {
        return new PaymentReqDTO.Webhook(
                "PAYMENT_STATUS_CHANGED",
                new PaymentReqDTO.Webhook.WebhookData(PAYMENT_KEY, ORDER_ID, status));
    }

    private PaymentResDTO.TossConfirm tossConfirmOf(String status) {
        return new PaymentResDTO.TossConfirm(
                PAYMENT_KEY, ORDER_ID, "카드", status, 155_000L, OffsetDateTime.now());
    }

    @Test
    void DONE_상태면_결제완료로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.getPayment(PAYMENT_KEY)).willReturn(tossConfirmOf("DONE"));

        paymentWebhookService.handle(webhookOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void 이미_PAID인_결제는_DONE_웹훅을_받아도_그대로_유지한다() {
        Payment payment = paymentOf(PaymentStatus.PAID);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.getPayment(PAYMENT_KEY)).willReturn(tossConfirmOf("DONE"));

        paymentWebhookService.handle(webhookOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void EXPIRED_상태면_결제를_만료로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.getPayment(PAYMENT_KEY)).willReturn(tossConfirmOf("EXPIRED"));

        paymentWebhookService.handle(webhookOf("EXPIRED"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test
    void ABORTED_상태면_결제를_실패로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.getPayment(PAYMENT_KEY)).willReturn(tossConfirmOf("ABORTED"));

        paymentWebhookService.handle(webhookOf("ABORTED"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void 대상_결제를_찾을_수_없으면_조용히_무시한다() {
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());

        paymentWebhookService.handle(webhookOf("DONE"));

        verify(tossPaymentClient, never()).getPayment(any());
    }

    @Test
    void PAYMENT_STATUS_CHANGED가_아닌_이벤트는_무시한다() {
        PaymentReqDTO.Webhook payload = new PaymentReqDTO.Webhook(
                "OTHER_EVENT", new PaymentReqDTO.Webhook.WebhookData(PAYMENT_KEY, ORDER_ID, "DONE"));

        paymentWebhookService.handle(payload);

        verify(paymentRepository, never()).findByOrderId(any());
    }

    @Test
    void 조회_결과의_주문번호가_다르면_반영하지_않는다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.getPayment(PAYMENT_KEY)).willReturn(new PaymentResDTO.TossConfirm(
                PAYMENT_KEY, "OTHER_ORDER_ID", "카드", "DONE", 155_000L, OffsetDateTime.now()));

        paymentWebhookService.handle(webhookOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
