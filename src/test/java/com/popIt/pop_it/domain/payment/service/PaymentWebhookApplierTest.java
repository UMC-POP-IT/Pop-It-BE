package com.popIt.pop_it.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
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
class PaymentWebhookApplierTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentWebhookApplier paymentWebhookApplier;

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

    private PaymentResDTO.TossConfirmRes tossConfirmOf(String status) {
        return new PaymentResDTO.TossConfirmRes(
                PAYMENT_KEY, ORDER_ID, "카드", status, 155_000L, OffsetDateTime.now());
    }

    @Test
    void DONE_상태면_결제완료로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getContract().getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }

    @Test
    void 이미_PAID인_결제는_DONE_웹훅을_받아도_그대로_유지한다() {
        Payment payment = paymentOf(PaymentStatus.PAID);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void EXPIRED_상태면_결제를_만료로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(paymentRepository.markExpiredIfPending(PAYMENT_ID)).willReturn(1);

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("EXPIRED"));

        verify(paymentRepository).markExpiredIfPending(PAYMENT_ID);
    }

    @Test
    void ABORTED_상태면_결제를_실패로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(paymentRepository.markFailedIfPending(PAYMENT_ID)).willReturn(1);

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("ABORTED"));

        verify(paymentRepository).markFailedIfPending(PAYMENT_ID);
    }

    @Test
    void 이미_PENDING이_아닌_결제는_EXPIRED_웹훅을_받아도_유지한다() {
        Payment payment = paymentOf(PaymentStatus.PAID);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        // DB의 조건부 UPDATE(WHERE status='PENDING')가 실제 방어를 담당하므로,
        // 이미 PENDING이 아닌 경우를 0건으로 흉내낸다(Mockito 기본값과 동일하지만 의도를 명시).
        given(paymentRepository.markExpiredIfPending(PAYMENT_ID)).willReturn(0);

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("EXPIRED"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void 반영_대상_결제를_찾을_수_없으면_조용히_무시한다() {
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("DONE"));
        // 예외 없이 조용히 종료되면 성공
    }
}
