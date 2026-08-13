package com.popIt.pop_it.domain.reservation.scheduler;

import com.popIt.pop_it.domain.payment.service.PaymentService;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ReservationCheckoutSchedulerTest {

    @Autowired
    private ReservationCheckoutScheduler scheduler;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Clock clock;

    // 실제 PG 정산 없이 스케줄러의 상태 전이 로직만 검증하기 위해 목으로 대체
    @MockitoBean
    private PaymentService paymentService;

    private Long hostId;
    private User guest;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("scheduler-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("scheduler-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Space saveSpace() {
        return spaceRepository.save(Space.builder()
                .buildingName("테스트용빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("101동 101호")
                .deposit(1_000_000L)
                .pricePerDay(100_000)
                .availableStartDate(LocalDate.now(clock).minusMonths(1))
                .availableEndDate(LocalDate.now(clock).plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("스케줄러 테스트용 공간")
                .hostId(hostId)
                .build());
    }

    private Reservation saveReservation(boolean checkoutRejected, LocalDateTime checkoutRejectedAt,
                                         LocalDateTime checkoutSubmittedAt) {
        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.USAGE_COMPLETED)
                .startDate(LocalDate.now(clock).minusDays(10))
                .endDate(LocalDate.now(clock).minusDays(3))
                .usagePurpose("스케줄러 테스트")
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .checkoutRejected(checkoutRejected)
                .checkoutRejectedAt(checkoutRejectedAt)
                .checkoutSubmittedAt(checkoutSubmittedAt)
                .space(saveSpace())
                .user(guest)
                .build();

        return reservationRepository.save(reservation);
    }

    private Reservation saveReservation(ReservationStatus status, LocalDate startDate, LocalDate endDate) {
        Reservation reservation = Reservation.builder()
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .usagePurpose("스케줄러 테스트")
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .space(saveSpace())
                .user(guest)
                .build();

        return reservationRepository.save(reservation);
    }

    @Test
    void 거절_후_24시간_지나면_자동으로_퇴실승인된다() {
        // given: 호스트가 거절한 지 25시간 지났고, 게스트는 재제출하지 않음
        Reservation reservation = saveReservation(true, LocalDateTime.now(clock).minusHours(25), null);

        // when
        scheduler.autoApproveCheckouts();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CHECKOUT_COMPLETED);
    }

    @Test
    void 거절_후_24시간_안지났으면_아직_자동승인되지_않는다() {
        // given: 거절한 지 23시간(아직 24h 미경과)
        Reservation reservation = saveReservation(true, LocalDateTime.now(clock).minusHours(23), null);

        // when
        scheduler.autoApproveCheckouts();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.USAGE_COMPLETED);
    }

    @Test
    void 거절됐다가_재제출하면_거절타임아웃_큐에서_빠지고_제출시각_기준으로_다시_대기한다() {
        // given: 거절된 지는 25시간 지났지만(오래된 checkoutRejectedAt),
        // 게스트가 방금 재제출해서 checkoutRejected=false, checkoutSubmittedAt=방금
        Reservation reservation = saveReservation(false, LocalDateTime.now(clock).minusHours(25), LocalDateTime.now(clock));

        // when
        scheduler.autoApproveCheckouts();

        // then: 재제출 시각 기준으로는 아직 24h가 안 지났으므로 자동승인되면 안 됨
        // (checkoutRejectedAt이 과거여도 checkoutRejected=false라 거절-타임아웃 큐 대상이 아님을 검증)
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.USAGE_COMPLETED);
    }

    @Test
    void 정상_제출_후_24시간_지나면_기존_로직대로_자동승인된다() {
        // given: 거절 이력 없이 정상 제출, 제출 시각으로부터 25시간 경과
        Reservation reservation = saveReservation(false, null, LocalDateTime.now(clock).minusHours(25));

        // when
        scheduler.autoApproveCheckouts();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CHECKOUT_COMPLETED);
    }

    @Test
    void 사진_미제출_상태로_24시간_지나면_기존_로직대로_자동승인된다() {
        // given: 사진 제출도, 거절 이력도 없이 이용완료 상태(endDate 기준 이미 3일 지남 = 24h 조건 충족)
        Reservation reservation = saveReservation(false, null, null);

        // when
        scheduler.autoApproveCheckouts();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CHECKOUT_COMPLETED);
    }

    @Test
    void 이용_시작일_도래하면_사용중으로_전환된다() {
        // given: 결제완료 상태, 이용 시작일이 오늘(이미 도래)
        Reservation reservation = saveReservation(
                ReservationStatus.PAYMENT_COMPLETED, LocalDate.now(clock), LocalDate.now(clock).plusDays(5));

        // when
        scheduler.startUsagePeriod();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.IN_USE);
    }

    @Test
    void 계약만_완료되고_결제_안된_상태는_이용_시작일이_지나도_사용중으로_전환되지_않는다() {
        // given: 계약완료(결제 전) 상태, 이용 시작일이 오늘(이미 도래)
        Reservation reservation = saveReservation(
                ReservationStatus.CONTRACT_COMPLETED, LocalDate.now(clock), LocalDate.now(clock).plusDays(5));

        // when
        scheduler.startUsagePeriod();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONTRACT_COMPLETED);
    }

    @Test
    void 예약일까지_결제_안하면_자동취소된다() {
        // given: 계약완료(결제 전) 상태, 이용 시작일이 오늘(이미 도래)
        Reservation reservation = saveReservation(
                ReservationStatus.CONTRACT_COMPLETED, LocalDate.now(clock), LocalDate.now(clock).plusDays(5));

        // when
        scheduler.cancelUnpaidReservations();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void 이용_기간_종료되면_이용완료로_전환된다() {
        // given: 사용중 상태, 이용 종료일이 어제(이미 지남)
        Reservation reservation = saveReservation(
                ReservationStatus.IN_USE, LocalDate.now(clock).minusDays(5), LocalDate.now(clock).minusDays(1));

        // when
        scheduler.completeUsagePeriod();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.USAGE_COMPLETED);
    }

    @Test
    void 예약일까지_승인_안하면_자동취소된다() {
        // given: 승인대기 상태, 예약 시작일이 오늘(이미 도래)
        Reservation reservation = saveReservation(
                ReservationStatus.PENDING_APPROVAL, LocalDate.now(clock), LocalDate.now(clock).plusDays(5));

        // when
        scheduler.cancelUnapprovedReservations();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void 예약일_전이면_승인_안해도_취소되지_않는다() {
        // given: 승인대기 상태, 예약 시작일이 아직 안 옴
        Reservation reservation = saveReservation(
                ReservationStatus.PENDING_APPROVAL, LocalDate.now(clock).plusDays(1), LocalDate.now(clock).plusDays(5));

        // when
        scheduler.cancelUnapprovedReservations();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
    }

    @Test
    void 예약일까지_계약_안하면_자동취소된다() {
        // given: 승인완료 상태, 예약 시작일이 오늘(이미 도래), 계약 미체결
        Reservation reservation = saveReservation(
                ReservationStatus.APPROVED, LocalDate.now(clock), LocalDate.now(clock).plusDays(5));

        // when
        scheduler.cancelUncontractedReservations();

        // then
        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
