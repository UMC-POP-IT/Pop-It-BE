package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.payment.service.PaymentService;
import com.popIt.pop_it.domain.reservation.dto.ReservationReqDTO;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.CheckoutImage;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.CheckoutImageRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ReservationCheckoutFlowTest {

    @Autowired
    private ReservationCommandService reservationCommandService;
    @Autowired
    private ReservationQueryService reservationQueryService;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private CheckoutImageRepository checkoutImageRepository;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private UserRepository userRepository;

    // 실제 PG 정산 없이 상태 전이 로직만 검증하기 위해 목으로 대체
    @MockitoBean
    private PaymentService paymentService;

    private Long hostId;
    private Long guestId;
    private Long otherUserId;
    private Space space;
    private User guest;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("checkout-flow-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("checkout-flow-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
        guestId = guest.getUserId();

        User other = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("checkout-flow-other-uid")
                .nickname("other")
                .currentMode(UserMode.GUEST)
                .build());
        otherUserId = other.getUserId();

        space = spaceRepository.save(Space.builder()
                .buildingName("퇴실 플로우 테스트용 빌딩")
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
                .availableStartDate(LocalDate.now().minusMonths(1))
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("퇴실 플로우 테스트용 공간")
                .hostId(hostId)
                .build());
    }

    @AfterEach
    void tearDown() {
        checkoutImageRepository.deleteAll();
        reservationRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Reservation saveUsageCompletedReservation() {
        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.USAGE_COMPLETED)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(3))
                .usagePurpose("퇴실 플로우 테스트")
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .space(space)
                .user(guest)
                .build();

        return reservationRepository.save(reservation);
    }

    private ReservationReqDTO.ReservationCheckoutReq photoReq(String... urls) {
        return new ReservationReqDTO.ReservationCheckoutReq(List.of(urls));
    }

    @Test
    void 퇴실_증빙_제출하면_사진이_활성_상태로_저장된다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();

        // when
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/1.jpg", "https://s3/2.jpg"));

        // then
        List<CheckoutImage> images = checkoutImageRepository.findAllByReservationIdAndIsActiveTrueOrderBySortOrder(reservation.getId());
        assertThat(images).hasSize(2);
        assertThat(images).allMatch(CheckoutImage::getIsActive);
    }

    @Test
    void 퇴실_거절하면_사진이_삭제되지_않고_비활성화만_된다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/1.jpg"));

        // when
        reservationCommandService.rejectCheckout(reservation.getId(), hostId);

        // then: 하드 삭제되지 않고 DB에 그대로 남아있되 isActive만 false로 전환
        List<CheckoutImage> allImages = checkoutImageRepository.findAll();
        assertThat(allImages).hasSize(1);
        assertThat(allImages.get(0).getIsActive()).isFalse();
        assertThat(allImages.get(0).getRejectedAt()).isNotNull();

        List<CheckoutImage> activeImages = checkoutImageRepository.findAllByReservationIdAndIsActiveTrueOrderBySortOrder(reservation.getId());
        assertThat(activeImages).isEmpty();
    }

    @Test
    void 거절_후_재제출하면_이전_사진은_비활성으로_남고_새_사진만_활성화된다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/old.jpg"));
        reservationCommandService.rejectCheckout(reservation.getId(), hostId);

        // when
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/new1.jpg", "https://s3/new2.jpg"));

        // then
        List<CheckoutImage> activeImages = checkoutImageRepository.findAllByReservationIdAndIsActiveTrueOrderBySortOrder(reservation.getId());
        assertThat(activeImages).extracting(CheckoutImage::getCheckoutImageUrl)
                .containsExactly("https://s3/new1.jpg", "https://s3/new2.jpg");

        List<CheckoutImage> allImages = checkoutImageRepository.findAll();
        assertThat(allImages).hasSize(3); // old 1장(비활성) + new 2장(활성)
    }

    @Test
    void 게스트_사진_조회는_거절_상태가_아니면_현재_유효한_사진을_반환한다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/1.jpg"));

        // when
        ReservationResDTO.ReservationCheckoutPhotosRes result =
                reservationQueryService.getCheckoutPhotosForGuest(reservation.getId(), guestId);

        // then
        assertThat(result.checkoutRejected()).isFalse();
        assertThat(result.photoUrls()).containsExactly("https://s3/1.jpg");
    }

    @Test
    void 게스트_사진_조회는_거절_상태면_가장_최근_거절_배치만_반환한다() {
        // given: 1차 제출 -> 거절 -> 2차 제출 -> 거절 (거절 이력 2번, 최근 배치만 보여야 함)
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/first.jpg"));
        reservationCommandService.rejectCheckout(reservation.getId(), hostId);
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/second.jpg"));
        reservationCommandService.rejectCheckout(reservation.getId(), hostId);

        // when
        ReservationResDTO.ReservationCheckoutPhotosRes result =
                reservationQueryService.getCheckoutPhotosForGuest(reservation.getId(), guestId);

        // then: 1차(first.jpg)가 아니라 가장 최근인 2차(second.jpg)만 반환
        assertThat(result.checkoutRejected()).isTrue();
        assertThat(result.photoUrls()).containsExactly("https://s3/second.jpg");
    }

    @Test
    void 본인_예약이_아니면_게스트_사진_조회시_접근이_거부된다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/1.jpg"));

        // when & then
        assertThatThrownBy(() -> reservationQueryService.getCheckoutPhotosForGuest(reservation.getId(), otherUserId))
                .isInstanceOf(ProjectException.class)
                .satisfies(e -> assertThat(((ProjectException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.RESERVATION_ACCESS_DENIED));
    }

    @Test
    void 퇴실_승인_여부_조회는_미제출_상태를_null_제출시각으로_반환한다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();

        // when
        ReservationResDTO.ReservationCheckoutApprovalRes result =
                reservationQueryService.getCheckoutApproval(reservation.getId(), guestId);

        // then
        assertThat(result.status()).isEqualTo(ReservationStatus.USAGE_COMPLETED);
        assertThat(result.checkoutRejected()).isFalse();
        assertThat(result.checkoutSubmittedAt()).isNull();
        assertThat(result.checkoutRejectedAt()).isNull();
    }

    @Test
    void 퇴실_승인_여부_조회는_거절_상태를_반영한다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/1.jpg"));
        reservationCommandService.rejectCheckout(reservation.getId(), hostId);

        // when
        ReservationResDTO.ReservationCheckoutApprovalRes result =
                reservationQueryService.getCheckoutApproval(reservation.getId(), guestId);

        // then
        assertThat(result.checkoutRejected()).isTrue();
        assertThat(result.checkoutRejectedAt()).isNotNull();
        assertThat(result.status()).isEqualTo(ReservationStatus.USAGE_COMPLETED); // 아직 CHECKOUT_COMPLETED로 전이되지 않음
    }

    @Test
    void 퇴실_승인_여부_조회는_승인_완료_상태를_반영한다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/1.jpg"));

        // when
        reservationCommandService.approveCheckout(reservation.getId(), hostId);
        ReservationResDTO.ReservationCheckoutApprovalRes result =
                reservationQueryService.getCheckoutApproval(reservation.getId(), guestId);

        // then
        assertThat(result.status()).isEqualTo(ReservationStatus.CHECKOUT_COMPLETED);
    }

    @Test
    void 퇴실_승인_여부_조회는_호스트도_조회할_수_있다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();

        // when & then: 예외 없이 조회되어야 함
        ReservationResDTO.ReservationCheckoutApprovalRes result =
                reservationQueryService.getCheckoutApproval(reservation.getId(), hostId);
        assertThat(result.reservationId()).isEqualTo(reservation.getId());
    }

    @Test
    void 게스트_호스트_둘_다_아니면_퇴실_승인_여부_조회가_거부된다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();

        // when & then
        assertThatThrownBy(() -> reservationQueryService.getCheckoutApproval(reservation.getId(), otherUserId))
                .isInstanceOf(ProjectException.class)
                .satisfies(e -> assertThat(((ProjectException) e).getErrorCode())
                        .isEqualTo(ReservationErrorCode.RESERVATION_ACCESS_DENIED));
    }

    @Test
    void 거절된_사진이_있어도_isPhotoVerified_배지_판단에는_포함되지_않는다() {
        // given
        Reservation reservation = saveUsageCompletedReservation();
        reservationCommandService.submitCheckout(reservation.getId(), guestId, photoReq("https://s3/1.jpg"));
        reservationCommandService.rejectCheckout(reservation.getId(), hostId);

        // when: 거절 후 재제출 전이라 활성 사진이 없는 상태
        List<Long> verifiedIds = checkoutImageRepository.findVerifiedReservationIds(List.of(reservation.getId()));

        // then
        assertThat(verifiedIds).doesNotContain(reservation.getId());
    }
}
