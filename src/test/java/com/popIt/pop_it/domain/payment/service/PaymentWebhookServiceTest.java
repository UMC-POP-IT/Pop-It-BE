package com.popIt.pop_it.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
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

// handle()의 책임(검증/조회/토스 재확인 후 위임)만 검증한다.
@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private PaymentWebhookApplier paymentWebhookApplier;

    @InjectMocks
    private PaymentWebhookService paymentWebhookService;

    private static final Long PAYMENT_ID = 1L;
    private static final String ORDER_ID = "ORDER_1_abcdefabcdef";
    private static final String PAYMENT_KEY = "payment-key-1";

    private Payment paymentOf(PaymentStatus status) {
        Contract contract = Contract.builder()
                .id(1L)
                .status(ContractStatus.PENDING_PAYMENT)
                .build();
        return Payment.builder()
                .id(PAYMENT_ID)
                .status(status)
                .orderId(ORDER_ID)
                .idempotencyKey("idem-key-1")
                .contract(contract)
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
    void 검증을_통과하면_applier에_반영을_위임한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        PaymentResDTO.TossConfirm actual = tossConfirmOf("DONE");
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.getPayment(PAYMENT_KEY)).willReturn(actual);

        paymentWebhookService.handle(webhookOf("DONE"));

        verify(paymentWebhookApplier).apply(PAYMENT_ID, actual);
    }

    @Test
    void 대상_결제를_찾을_수_없으면_조용히_무시한다() {
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());

        paymentWebhookService.handle(webhookOf("DONE"));

        verify(tossPaymentClient, never()).getPayment(any());
        verify(paymentWebhookApplier, never()).apply(any(), any());
    }

    @Test
    void PAYMENT_STATUS_CHANGED가_아닌_이벤트는_무시한다() {
        PaymentReqDTO.Webhook payload = new PaymentReqDTO.Webhook(
                "OTHER_EVENT", new PaymentReqDTO.Webhook.WebhookData(PAYMENT_KEY, ORDER_ID, "DONE"));

        paymentWebhookService.handle(payload);

        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentWebhookApplier, never()).apply(any(), any());
    }

    @Test
    void data가_없으면_조용히_무시한다() {
        PaymentReqDTO.Webhook payload = new PaymentReqDTO.Webhook("PAYMENT_STATUS_CHANGED", null);

        paymentWebhookService.handle(payload);

        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentWebhookApplier, never()).apply(any(), any());
    }

    @Test
    void 조회_결과의_주문번호가_다르면_반영하지_않는다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.getPayment(PAYMENT_KEY)).willReturn(new PaymentResDTO.TossConfirm(
                PAYMENT_KEY, "OTHER_ORDER_ID", "카드", "DONE", 155_000L, OffsetDateTime.now()));

        paymentWebhookService.handle(webhookOf("DONE"));

        verify(paymentWebhookApplier, never()).apply(any(), any());
    }
}
