package com.popIt.pop_it.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.payment.dto.PaymentPrepareResponseDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentErrorCode;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PaymentIdempotentSaver paymentIdempotentSaver;

    @InjectMocks
    private PaymentService paymentService;

    private static final Long CONTRACT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String IDEMPOTENCY_KEY = "idem-key-1";

    private Contract contractOf(Long contractId, ContractStatus status, Long ownerUserId) {
        User user = User.builder().userId(ownerUserId).build();
        Space space = Space.builder().buildingName("팝잇 빌딩").build();
        Reservation reservation = Reservation.builder()
                .rentalFee(100_000L)
                .deposit(50_000L)
                .space(space)
                .user(user)
                .build();
        return Contract.builder()
                .id(contractId)
                .status(status)
                .rentalFee(100_000L)
                .deposit(50_000L)
                .insuranceFee(5_000L)
                .totalPrice(155_000L)
                .reservation(reservation)
                .build();
    }

    private Payment paymentOf(Contract contract, PaymentStatus status, String orderId) {
        return Payment.builder()
                .id(1L)
                .status(status)
                .orderId(orderId)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .contract(contract)
                .build();
    }

    @Test
    void 신규_결제_준비에_성공한다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment savedPayment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.of(contract));
        given(paymentIdempotentSaver.save(any(Payment.class), eq(IDEMPOTENCY_KEY))).willReturn(savedPayment);

        PaymentPrepareResponseDTO result = paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID);

        assertThat(result.paymentId()).isEqualTo(1L);
        assertThat(result.orderId()).isEqualTo("ORDER_1_abc");
        assertThat(result.amount()).isEqualTo(155_000L);
        assertThat(result.rentFee()).isEqualTo(100_000L);
        assertThat(result.deposit()).isEqualTo(50_000L);
        assertThat(result.insuranceFee()).isEqualTo(5_000L);
        assertThat(result.status()).isEqualTo("PENDING");
        verify(paymentIdempotentSaver).save(any(Payment.class), eq(IDEMPOTENCY_KEY));
    }

    @Test
    void 계약을_찾을_수_없으면_예외() {
        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class);
        verify(paymentIdempotentSaver, never()).save(any(), any());
    }

    @Test
    void 본인_계약이_아니면_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, 999L);

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.of(contract));

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_FORBIDDEN);
        verify(paymentIdempotentSaver, never()).save(any(), any());
    }

    @Test
    void 결제_가능한_계약_상태가_아니면_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_SIGNATURE, USER_ID);

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.of(contract));

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_CONFLICT_PAYMENT);
        verify(paymentIdempotentSaver, never()).save(any(), any());
    }

    @Test
    void 같은_멱등키로_다른_계약이_들어오면_예외() {
        Contract originalContract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment existingPayment = paymentOf(originalContract, PaymentStatus.PENDING, "ORDER_1_abc");

        Long otherContractId = 2L;
        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.prepare(otherContractId, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_CONFLICT_KEY);
    }

    @Test
    void 같은_키로_재호출시_대기중이면_기존_결제를_그대로_반환한다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment existingPayment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(existingPayment));

        PaymentPrepareResponseDTO result = paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID);

        assertThat(result.paymentId()).isEqualTo(existingPayment.getId());
        assertThat(result.orderId()).isEqualTo("ORDER_1_abc");
        verify(paymentIdempotentSaver, never()).save(any(), any());
        verify(contractRepository, never()).findWithReservationAndUserById(any());
    }

    @Test
    void 같은_키로_재호출시_이미_결제완료면_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment existingPayment = paymentOf(contract, PaymentStatus.PAID, "ORDER_1_abc");

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_PAID);
    }

    @Test
    void 같은_키로_재호출시_실패건이면_재시도_안내_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment existingPayment = paymentOf(contract, PaymentStatus.FAILED, "ORDER_1_abc");

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_RETRYABLE);
    }
}
