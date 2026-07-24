package com.popIt.pop_it.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.contract.service.ContractService;
import com.popIt.pop_it.domain.payment.client.HostPayoutClient;
import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.enums.SettlementStepStatus;
import com.popIt.pop_it.domain.payment.exception.code.PaymentErrorCode;
import com.popIt.pop_it.domain.payment.exception.code.TossErrorCode;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractService contractService;

    @Mock
    private PaymentIdempotentSaver paymentIdempotentSaver;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private HostPayoutClient hostPayoutClient;

    @Mock
    private PaymentSettlementRecorder paymentSettlementRecorder;

    @InjectMocks
    private PaymentService paymentService;

    private static final Long CONTRACT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long HOST_ID = 999L;
    private static final String IDEMPOTENCY_KEY = "idem-key-1";
    private static final Long PAYMENT_ID = 1L;

    private Contract contractOf(Long contractId, ContractStatus status, Long ownerUserId) {
        User user = User.builder().userId(ownerUserId).build();
        Space space = Space.builder().buildingName("팝잇 빌딩").hostId(HOST_ID).build();
        Reservation reservation = Reservation.builder()
                .rentalFee(100_000L)
                .deposit(50_000L)
                .platformFee(10_000L)
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

    // 정산(settle) 테스트 전용: 실제 토스 승인 후에만 채워지는 paymentKey를 가진 승인 완료 결제
    private Payment approvedPaymentOf(Contract contract, String orderId) {
        return Payment.builder()
                .id(PAYMENT_ID)
                .status(PaymentStatus.PAID)
                .orderId(orderId)
                .paymentKey("paymentKey-1")
                .idempotencyKey(IDEMPOTENCY_KEY)
                .contract(contract)
                .build();
    }

    // 실제 PaymentSettlementRecorder는 별도 트랜잭션에서 DB에 반영하지만,
    // 단위 테스트에서는 mutation을 같은 payment 인스턴스에 바로 적용해 결과를 검증한다.
    private void stubSettlementRecorderToMutate(Payment payment) {
        willAnswer(invocation -> {
            Consumer<Payment> mutation = invocation.getArgument(1);
            mutation.accept(payment);
            return null;
        }).given(paymentSettlementRecorder).update(eq(payment.getId()), any());
    }

    // 정상 흐름 테스트에서는 선점(claim)이 항상 성공해 외부 API 호출까지 이어지도록 스텁한다.
    private void stubClaimsToSucceed(Long paymentId) {
        given(paymentSettlementRecorder.claimHostPayout(paymentId)).willReturn(true);
        given(paymentSettlementRecorder.claimDepositRefund(paymentId)).willReturn(true);
    }

    @Test
    void 신규_결제_준비에_성공한다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment savedPayment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.of(contract));
        given(paymentIdempotentSaver.save(any(Payment.class))).willReturn(savedPayment);

        PaymentResDTO.Prepare result = paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID);

        assertThat(result.paymentId()).isEqualTo(1L);
        assertThat(result.orderId()).isEqualTo("ORDER_1_abc");
        assertThat(result.amount()).isEqualTo(155_000L);
        assertThat(result.rentFee()).isEqualTo(100_000L);
        assertThat(result.deposit()).isEqualTo(50_000L);
        assertThat(result.insuranceFee()).isEqualTo(5_000L);
        assertThat(result.status()).isEqualTo("PENDING");
        verify(paymentIdempotentSaver).save(any(Payment.class));
    }

    @Test
    void 다른_멱등키로_동시_요청이_먼저_PENDING을_생성하면_그_결제를_반환한다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment concurrentlyCreatedPayment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.of(contract));
        given(paymentIdempotentSaver.save(any(Payment.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));
        given(paymentRepository.findByContractIdAndStatus(CONTRACT_ID, PaymentStatus.PENDING))
                .willReturn(Optional.of(concurrentlyCreatedPayment));

        PaymentResDTO.Prepare result = paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID);

        assertThat(result.paymentId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void 같은_멱등키로_동시_요청이_다른_계약의_결제를_먼저_생성했으면_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Long otherContractId = 2L;
        Contract otherContract = contractOf(otherContractId, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment otherContractsPayment = paymentOf(otherContract, PaymentStatus.PENDING, "ORDER_2_abc");

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .willReturn(Optional.empty(), Optional.of(otherContractsPayment));
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.of(contract));
        given(paymentIdempotentSaver.save(any(Payment.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_CONFLICT_KEY);
    }

    @Test
    void 계약을_찾을_수_없으면_예외() {
        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class);
        verify(paymentIdempotentSaver, never()).save(any());
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
        verify(paymentIdempotentSaver, never()).save(any());
    }

    @Test
    void 결제_가능한_계약_상태가_아니면_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.HOST_SIGNATURE_PENDING, USER_ID);

        given(paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(contractRepository.findWithReservationAndUserById(CONTRACT_ID)).willReturn(Optional.of(contract));

        assertThatThrownBy(() -> paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_CONFLICT_PAYMENT);
        verify(paymentIdempotentSaver, never()).save(any());
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

        PaymentResDTO.Prepare result = paymentService.prepare(CONTRACT_ID, IDEMPOTENCY_KEY, USER_ID);

        assertThat(result.paymentId()).isEqualTo(existingPayment.getId());
        assertThat(result.orderId()).isEqualTo("ORDER_1_abc");
        verify(paymentIdempotentSaver, never()).save(any());
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

    @Test
    void 결제_승인에_성공한다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 155_000L);
        OffsetDateTime approvedAt = OffsetDateTime.now();

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.confirm("paymentKey-1", "ORDER_1_abc", 155_000L))
                .willReturn(new PaymentResDTO.TossConfirm(
                        "paymentKey-1", "ORDER_1_abc", "카드", "DONE", 155_000L, approvedAt));

        PaymentResDTO.Confirm result = paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID);

        assertThat(result.paymentId()).isEqualTo(payment.getId());
        assertThat(result.orderId()).isEqualTo("ORDER_1_abc");
        assertThat(result.method()).isEqualTo(PaymentMethod.CARD.name());
        assertThat(result.status()).isEqualTo(PaymentStatus.PAID.name());
        assertThat(payment.getPaymentKey()).isEqualTo("paymentKey-1");
        assertThat(payment.getPaidAt()).isEqualTo(approvedAt.toLocalDateTime());
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }

    @Test
    void 결제를_찾을_수_없으면_승인_예외() {
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 155_000L);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void 본인_결제가_아니면_승인_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 155_000L);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, 999L))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_FORBIDDEN);
    }

    @Test
    void 실패한_결제는_재승인_대상이_아니다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.FAILED, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 155_000L);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_RETRYABLE);
        verify(tossPaymentClient, never()).confirm(any(), any(), any());
    }

    @Test
    void 만료된_결제는_재승인_대상이_아니다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.EXPIRED, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 155_000L);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_RETRYABLE);
        verify(tossPaymentClient, never()).confirm(any(), any(), any());
    }

    @Test
    void 요청한_주문번호가_다르면_승인_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_다른값", 155_000L);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_ORDER_MISMATCH);
    }

    @Test
    void 요청한_금액이_계약_금액과_다르면_승인_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 1_000L);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    void 토스_승인_실패시_결제가_실패_처리되고_예외가_전파된다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 155_000L);
        TossErrorCode tossErrorCode = new TossErrorCode(
                HttpStatus.BAD_REQUEST, "INVALID_CARD_NUMBER", "카드번호를 다시 확인해주세요.");

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.confirm("paymentKey-1", "ORDER_1_abc", 155_000L))
                .willThrow(new ProjectException(tossErrorCode));

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(tossErrorCode);
        verify(paymentSettlementRecorder).markConfirmFailedIfPending(PAYMENT_ID);
    }

    @Test
    void 동시_confirm_요청_중_하나가_이미_PAID로_커밋했으면_실패_기록이_덮어쓰지_않는다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");
        PaymentReqDTO.Confirm reqDTO = new PaymentReqDTO.Confirm("paymentKey-1", "ORDER_1_abc", 155_000L);
        TossErrorCode tossErrorCode = new TossErrorCode(
                HttpStatus.BAD_REQUEST, "ALREADY_PROCESSED_PAYMENT", "이미 처리된 결제입니다.");

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(tossPaymentClient.confirm("paymentKey-1", "ORDER_1_abc", 155_000L))
                .willThrow(new ProjectException(tossErrorCode));
        // 다른 confirm() 요청이 이미 PAID로 커밋해, 조건부 UPDATE(WHERE status='PENDING')가 0건에 그친 상황을 흉내낸다.
        given(paymentSettlementRecorder.markConfirmFailedIfPending(PAYMENT_ID)).willReturn(false);

        assertThatThrownBy(() -> paymentService.confirm(PAYMENT_ID, reqDTO, USER_ID))
                .isInstanceOf(ProjectException.class);

        // 이 결제 인스턴스는 confirm()의 애초 조회 결과일 뿐이라 여기선 상태 변화가 없는 게 맞다.
        // 실제 덮어쓰기 방지는 DB의 조건부 UPDATE가 담당하며, 그 결과(false)만 이 테스트에서 확인한다.
        verify(paymentSettlementRecorder).markConfirmFailedIfPending(PAYMENT_ID);
    }

    @Test
    void 정산에_성공하면_호스트지급과_보증금환불이_둘다_완료된다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = approvedPaymentOf(contract, "ORDER_1_abc");
        stubSettlementRecorderToMutate(payment);
        stubClaimsToSucceed(PAYMENT_ID);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentService.settle(PAYMENT_ID);

        assertThat(payment.getHostPayoutStatus()).isEqualTo(SettlementStepStatus.DONE);
        assertThat(payment.getDepositRefundStatus()).isEqualTo(SettlementStepStatus.DONE);
        verify(hostPayoutClient).payout("ORDER_1_abc-HOST", HOST_ID, 90_000L);
        verify(tossPaymentClient).cancelPartial("paymentKey-1", 50_000L, "퇴실 승인에 따른 보증금 환불", "ORDER_1_abc-REFUND");
    }

    @Test
    void 결제를_찾을_수_없으면_정산_예외() {
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.settle(PAYMENT_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void 결제완료_상태가_아니면_정산_예외() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = paymentOf(contract, PaymentStatus.PENDING, "ORDER_1_abc");

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.settle(PAYMENT_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_PAID);
    }

    @Test
    void 호스트지급이_실패해도_보증금환불은_시도되고_정산_예외가_발생한다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = approvedPaymentOf(contract, "ORDER_1_abc");
        stubSettlementRecorderToMutate(payment);
        stubClaimsToSucceed(PAYMENT_ID);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        willThrow(new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR))
                .given(hostPayoutClient).payout(any(), any(), any());

        assertThatThrownBy(() -> paymentService.settle(PAYMENT_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_SETTLEMENT_FAILED);

        assertThat(payment.getHostPayoutStatus()).isEqualTo(SettlementStepStatus.FAILED);
        assertThat(payment.getDepositRefundStatus()).isEqualTo(SettlementStepStatus.DONE);
        verify(tossPaymentClient).cancelPartial(any(), any(), any(), any());
    }

    @Test
    void 보증금환불이_실패해도_호스트지급_결과는_유지되고_정산_예외가_발생한다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = approvedPaymentOf(contract, "ORDER_1_abc");
        stubSettlementRecorderToMutate(payment);
        stubClaimsToSucceed(PAYMENT_ID);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        willThrow(new ProjectException(new TossErrorCode(
                HttpStatus.BAD_REQUEST, "NOT_CANCELABLE_AMOUNT", "취소 가능 금액을 초과했습니다.")))
                .given(tossPaymentClient).cancelPartial(any(), any(), any(), any());

        assertThatThrownBy(() -> paymentService.settle(PAYMENT_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_SETTLEMENT_FAILED);

        assertThat(payment.getHostPayoutStatus()).isEqualTo(SettlementStepStatus.DONE);
        assertThat(payment.getDepositRefundStatus()).isEqualTo(SettlementStepStatus.FAILED);
        verify(hostPayoutClient).payout(any(), any(), any());
    }

    @Test
    void 이미_완료된_단계는_재시도하지_않는다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = Payment.builder()
                .id(PAYMENT_ID)
                .status(PaymentStatus.PAID)
                .orderId("ORDER_1_abc")
                .paymentKey("paymentKey-1")
                .idempotencyKey(IDEMPOTENCY_KEY)
                .contract(contract)
                .hostPayoutStatus(SettlementStepStatus.DONE)
                .depositRefundStatus(SettlementStepStatus.PENDING)
                .build();
        stubSettlementRecorderToMutate(payment);
        given(paymentSettlementRecorder.claimDepositRefund(PAYMENT_ID)).willReturn(true);

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

        paymentService.settle(PAYMENT_ID);

        assertThat(payment.getDepositRefundStatus()).isEqualTo(SettlementStepStatus.DONE);
        verify(hostPayoutClient, never()).payout(any(), any(), any());
        verify(paymentSettlementRecorder, never()).claimHostPayout(any());
        verify(tossPaymentClient).cancelPartial(any(), any(), any(), any());
    }

    @Test
    void 다른_실행이_이미_처리중이면_외부_API를_다시_호출하지_않는다() {
        Contract contract = contractOf(CONTRACT_ID, ContractStatus.PENDING_PAYMENT, USER_ID);
        Payment payment = approvedPaymentOf(contract, "ORDER_1_abc");

        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        // 선점 실패 = 다른 실행이 이미 PROCESSING으로 가져갔거나 그 사이 DONE으로 끝냈다는 뜻
        given(paymentSettlementRecorder.claimHostPayout(PAYMENT_ID)).willReturn(false);
        given(paymentSettlementRecorder.claimDepositRefund(PAYMENT_ID)).willReturn(false);

        assertThatThrownBy(() -> paymentService.settle(PAYMENT_ID))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_SETTLEMENT_FAILED);

        verify(hostPayoutClient, never()).payout(any(), any(), any());
        verify(tossPaymentClient, never()).cancelPartial(any(), any(), any(), any());
    }
}
