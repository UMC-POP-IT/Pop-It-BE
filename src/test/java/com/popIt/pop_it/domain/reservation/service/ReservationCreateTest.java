package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.reservation.dto.ReservationReqDTO;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.*;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ReservationCreateTest {

    @Autowired
    private ReservationCommandService reservationCommandService;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    private Long hostId;
    private Long guestId;
    private Long spaceId;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("create-test-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        User guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("create-test-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
        guestId = guest.getUserId();

        Space space = spaceRepository.save(Space.builder()
                .buildingName("예약 생성 테스트용 빌딩")
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
                .description("예약 생성 테스트용 공간")
                .hostId(hostId)
                .build());
        spaceId = space.getId();
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 예약_생성_응답에_reservationId가_포함된다() {
        ReservationReqDTO.ReservationCreateReq request = new ReservationReqDTO.ReservationCreateReq(
                spaceId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), "예약 생성 테스트"
        );

        ReservationResDTO.ReservationCreateRes result = reservationCommandService.createReservation(guestId, request);

        assertThat(result.reservationId()).isNotNull();
    }

    @Test
    void 호스트는_자신의_공간을_예약할_수_없다() {
        ReservationReqDTO.ReservationCreateReq request = new ReservationReqDTO.ReservationCreateReq(
                spaceId, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), "본인 공간 예약 시도"
        );

        assertThatThrownBy(() -> reservationCommandService.createReservation(hostId, request))
                .isInstanceOf(ProjectException.class)
                .satisfies(e -> assertThat(((ProjectException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.RESERVATION_SELF_BOOKING_NOT_ALLOWED));
    }

    @Test
    void 당일_예약은_불가능하다() {
        ReservationReqDTO.ReservationCreateReq request = new ReservationReqDTO.ReservationCreateReq(
                spaceId, LocalDate.now(), LocalDate.now().plusDays(2), "당일 예약 시도"
        );

        assertThatThrownBy(() -> reservationCommandService.createReservation(guestId, request))
                .isInstanceOf(ProjectException.class)
                .satisfies(e -> assertThat(((ProjectException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.RESERVATION_INVALID_DATE));
    }

    private Reservation saveReservationWithStatus(ReservationStatus status) {
        return reservationRepository.save(Reservation.builder()
                .status(status)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .usagePurpose("취소 가능 상태 테스트")
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .checkoutRejected(false)
                .space(spaceRepository.findById(spaceId).orElseThrow())
                .user(userRepository.findById(guestId).orElseThrow())
                .build());
    }

    @Test
    void 계약완료_상태에서도_게스트가_예약을_취소할_수_있다() {
        Reservation reservation = saveReservationWithStatus(ReservationStatus.CONTRACT_COMPLETED);

        ReservationResDTO.ReservationStatusChangeRes result =
                reservationCommandService.cancelByGuest(reservation.getId(), guestId);

        assertThat(result.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void 결제완료_상태부터는_게스트가_예약을_취소할_수_없다() {
        Reservation reservation = saveReservationWithStatus(ReservationStatus.PAYMENT_COMPLETED);

        assertThatThrownBy(() -> reservationCommandService.cancelByGuest(reservation.getId(), guestId))
                .isInstanceOf(ProjectException.class)
                .satisfies(e -> assertThat(((ProjectException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.RESERVATION_CANCEL_NOT_ALLOWED));
    }
}
