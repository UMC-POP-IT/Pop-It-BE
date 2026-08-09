package com.popIt.pop_it.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookApplierTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ContractRepository contractRepository;

    private PaymentWebhookApplier paymentWebhookApplier;

    @BeforeEach
    void setUp() {
        // 실제 계약/예약 완료 로직(멱등/이중승인 판정)이 그대로 돌아야 검증이 의미가 있으므로
        // ContractCompletionService는 목이 아닌 실제 인스턴스를 쓴다.
        paymentWebhookApplier = new PaymentWebhookApplier(
                paymentRepository, new ContractCompletionService(contractRepository));
    }

    private static final Long PAYMENT_ID = 1L;
    private static final String ORDER_ID = "ORDER_1_abcdefabcdef";
    private static final String PAYMENT_KEY = "payment-key-1";

    // completeContractIfNeeded()가 계약을 완료 처리할 때 reservation.markPaymentCompleted()까지
    // 타므로, 그 전제조건(CONTRACT_COMPLETED)을 만족하는 reservation을 항상 붙여준다.
    private Payment paymentOf(PaymentStatus status, ContractStatus contractStatus) {
        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.CONTRACT_COMPLETED)
                .build();
        Contract contract = Contract.builder()
                .id(1L)
                .status(contractStatus)
                .reservation(reservation)
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
        Payment payment = paymentOf(PaymentStatus.PENDING, ContractStatus.PENDING_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getContract().getStatus()).isEqualTo(ContractStatus.COMPLETED);
        assertThat(payment.getContract().getReservation().getStatus())
                .isEqualTo(ReservationStatus.PAYMENT_COMPLETED);
    }

    @Test
    void 이미_PAID인_결제는_DONE_웹훅을_받아도_그대로_유지한다() {
        // 결제도 계약도 이미 정상 완료된, 흔한 웹훅 재전송/중복 도착 상황
        Payment payment = paymentOf(PaymentStatus.PAID, ContractStatus.COMPLETED);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getContract().getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }

    @Test
    void 이미_PAID인_결제는_계약_완료가_안_끝났으면_이어서_완료_처리한다() {
        // 직전 confirm()에서 결제는 PAID로 확정됐지만 계약 완료 단계가 실패해 남은 경우,
        // 뒤늦게 도착한 웹훅이 이어서 계약/예약 완료를 마무리해야 한다.
        Payment payment = paymentOf(PaymentStatus.PAID, ContractStatus.PENDING_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("DONE"));

        assertThat(payment.getContract().getStatus()).isEqualTo(ContractStatus.COMPLETED);
        assertThat(payment.getContract().getReservation().getStatus())
                .isEqualTo(ReservationStatus.PAYMENT_COMPLETED);
    }

    @Test
    void 계약이_다른_결제로_이미_완료됐으면_중복_승인으로_보고_계약을_건드리지_않는다() {
        // 이 결제는 아직 PENDING(또는 FAILED)인데, 같은 계약이 이미 다른 결제로 COMPLETED된 상태 -
        // 이중 청구 상황. 결제 자체는 실제로 승인된 사실이니 PAID로 남기되, 계약/예약은 그대로 둔다.
        Payment payment = paymentOf(PaymentStatus.PENDING, ContractStatus.COMPLETED);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("DONE"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getContract().getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }

    @Test
    void EXPIRED_상태면_결제를_만료로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING, ContractStatus.PENDING_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(paymentRepository.markExpiredIfPending(PAYMENT_ID)).willReturn(1);

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("EXPIRED"));

        verify(paymentRepository).markExpiredIfPending(PAYMENT_ID);
    }

    @Test
    void ABORTED_상태면_결제를_실패로_반영한다() {
        Payment payment = paymentOf(PaymentStatus.PENDING, ContractStatus.PENDING_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(paymentRepository.markFailedIfPending(PAYMENT_ID)).willReturn(1);

        paymentWebhookApplier.apply(PAYMENT_ID, tossConfirmOf("ABORTED"));

        verify(paymentRepository).markFailedIfPending(PAYMENT_ID);
    }

    @Test
    void 이미_PENDING이_아닌_결제는_EXPIRED_웹훅을_받아도_유지한다() {
        Payment payment = paymentOf(PaymentStatus.PAID, ContractStatus.COMPLETED);
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
