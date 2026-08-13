package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.payment.client.HostPayoutClient;
import com.popIt.pop_it.domain.payment.client.TossPaymentClient;
import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.enums.SettlementStepStatus;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.*;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// PaymentServiceTest(단위 테스트)는 PaymentRepository/PaymentSettlementRecorder를 모두 mock으로
// 대체하므로, skipDepositRefundIfPending()의 실제 조건부 UPDATE가 REQUIRES_NEW 트랜잭션에 커밋되고
// 그 이후 완전히 별도인 트랜잭션(영속성 컨텍스트)에서도 최신값(SKIPPED)으로 조회되는지는 검증하지 못한다.
// 실제 트랜잭션 경계와 영속성 컨텍스트를 쓰는 이 통합 테스트로 그 부분을 확인한다.
@SpringBootTest
class PaymentSettlementIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentService paymentService;
    // 패키지 프라이빗이라 이 테스트가 같은 패키지(payment.service)에 있어야 직접 접근할 수 있다.
    // claim*/skip* 같은 @Modifying 쿼리는 SimpleJpaRepository의 CRUD 메서드와 달리 자동으로
    // 트랜잭션이 열리지 않으므로, 프로덕션 코드와 동일하게 REQUIRES_NEW로 감싸는 이 레코더를 통해 호출한다.
    @Autowired
    private PaymentSettlementRecorder paymentSettlementRecorder;

    // 실제 외부 연동 없이 정산 로직만 검증하기 위해 외부 API 호출 지점을 목으로 대체한다.
    @MockitoBean
    private HostPayoutClient hostPayoutClient;
    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    private Long hostId;
    private Long guestId;
    private Long spaceId;
    private Long reservationId;
    private Long contractId;
    private Long paymentId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("settle-test-host-" + suffix)
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        User guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("settle-test-guest-" + suffix)
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
        guestId = guest.getUserId();

        Space space = spaceRepository.save(Space.builder()
                .buildingName("정산 통합테스트용 빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("101동 101호")
                .dong("합정동")
                .deposit(1_000_000L)
                .pricePerDay(100_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("정산 통합테스트용 공간")
                .hostId(hostId)
                .build());
        spaceId = space.getId();

        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(12);
        String usagePurpose = "정산 통합테스트";

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .status(ReservationStatus.APPROVED)
                .startDate(startDate)
                .endDate(endDate)
                .usagePurpose(usagePurpose)
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .space(space)
                .user(guest)
                .build());
        reservationId = reservation.getId();

        Contract contract = contractRepository.save(Contract.builder()
                .status(ContractStatus.COMPLETED)
                .hostId(hostId)
                .guestId(guestId)
                .spaceId(spaceId)
                .spaceName("정산 통합테스트용 빌딩")
                .roadAddress("테스트로 1")
                .startDate(startDate)
                .endDate(endDate)
                .usagePurpose(usagePurpose)
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .contentHash("test-content-hash-" + suffix)
                .reservation(reservation)
                .build());
        contractId = contract.getId();

        Payment payment = paymentRepository.save(Payment.builder()
                .status(PaymentStatus.PAID)
                .orderId("ORDER-" + suffix)
                .paymentKey("paymentKey-" + suffix)
                .idempotencyKey("IDEM-" + suffix)
                .contract(contract)
                .build());
        paymentId = payment.getId();
    }

    @AfterEach
    void tearDown() {
        safeDelete(() -> paymentRepository.deleteById(paymentId));
        safeDelete(() -> contractRepository.deleteById(contractId));
        safeDelete(() -> reservationRepository.deleteById(reservationId));
        safeDelete(() -> spaceRepository.deleteById(spaceId));
        safeDelete(() -> userRepository.deleteById(hostId));
        safeDelete(() -> userRepository.deleteById(guestId));
    }

    private void safeDelete(Runnable delete) {
        try {
            delete.run();
        } catch (Exception ignored) {
            // setUp이 중간에 실패해 일부만 만들어진 경우 등, 정리 자체의 실패는 무시한다.
        }
    }

    @Test
    void 호스트정산만_진행하면_보증금환불_SKIPPED가_실제_DB에_커밋되어_이후_전체_재시도에서_보증금환불을_다시_시도하지_않는다() {
        paymentService.settleHostPayoutOnly(paymentId);

        // settleHostPayoutOnly가 끝난 뒤, 완전히 새로운 트랜잭션(영속성 컨텍스트)으로 다시 조회해도
        // skipDepositRefundIfPending의 REQUIRES_NEW 커밋이 보이는지 확인한다.
        Payment afterHostOnlySettlement = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(afterHostOnlySettlement.getHostPayoutStatus()).isEqualTo(SettlementStepStatus.DONE);
        assertThat(afterHostOnlySettlement.getDepositRefundStatus()).isEqualTo(SettlementStepStatus.SKIPPED);

        // 재시도 스케줄러가 하듯 이후 전체 settle()을 다시 호출해도, 이미 SKIPPED로 커밋된
        // 보증금환불 단계는 재시도(토스 부분취소 호출)하지 않아야 한다.
        paymentService.settle(paymentId);

        verify(tossPaymentClient, never()).cancelPartial(any(), any(), any(), any());
    }

    @Test
    void 이미_처리중인_보증금환불_상태는_skip_조건부_UPDATE로_덮어쓰이지_않는다() {
        // 다른 경로가 먼저 보증금환불을 선점(PROCESSING)한 상황을 재현
        boolean claimed = paymentSettlementRecorder.claimDepositRefund(paymentId);
        assertThat(claimed).isTrue();

        paymentSettlementRecorder.skipDepositRefundIfPending(paymentId);

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getDepositRefundStatus())
                .isEqualTo(SettlementStepStatus.PROCESSING);
    }
}
