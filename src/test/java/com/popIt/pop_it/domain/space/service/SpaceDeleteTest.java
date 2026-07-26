package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SpaceDeleteTest {

    @Autowired private SpaceService spaceService;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserRepository userRepository;

    private static final Long OTHER_USER_ID = 999L;

    @Test
    @DisplayName("진행 중인 예약이 없으면 소프트 삭제된다")
    void delete_softDeletesSpace() {
        User host = saveUser("host-1");
        Space space = saveSpace(host.getUserId());

        SpaceResDTO.SpaceDeleteRes result = spaceService.deleteSpace(host.getUserId(), space.getId());

        assertThat(result.spaceId()).isEqualTo(space.getId());

        assertThat(spaceRepository.findByIdAndDeletedAtIsNull(space.getId())).isEmpty();
        Space raw = spaceRepository.findById(space.getId()).orElseThrow();
        assertThat(raw.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("취소·퇴실 완료된 예약만 있으면 삭제할 수 있다")
    void delete_withOnlyEndedReservations_succeeds() {
        User host = saveUser("host-2");
        User guest = saveUser("guest-2");
        Space space = saveSpace(host.getUserId());
        saveReservation(space, guest, ReservationStatus.CANCELLED);
        saveReservation(space, guest, ReservationStatus.CHECKOUT_COMPLETED);

        spaceService.deleteSpace(host.getUserId(), space.getId());

        assertThat(spaceRepository.findByIdAndDeletedAtIsNull(space.getId())).isEmpty();
    }

    @Test
    @DisplayName("진행 중인 예약이 있으면 삭제할 수 없다 (400)")
    void delete_withActiveReservation_throws() {
        User host = saveUser("host-3");
        User guest = saveUser("guest-3");
        Space space = saveSpace(host.getUserId());
        saveReservation(space, guest, ReservationStatus.PENDING_APPROVAL);

        assertThatThrownBy(() -> spaceService.deleteSpace(host.getUserId(), space.getId()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.SPACE_HAS_ACTIVE_RESERVATION);

        // 삭제 표시가 남지 않아야 한다
        assertThat(spaceRepository.findById(space.getId()).orElseThrow().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("이미 삭제한 공간을 다시 삭제하면 404")
    void delete_alreadyDeleted_throws() {
        User host = saveUser("host-4");
        Space space = saveSpace(host.getUserId());
        spaceService.deleteSpace(host.getUserId(), space.getId());

        assertThatThrownBy(() -> spaceService.deleteSpace(host.getUserId(), space.getId()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.SPACE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 공간이면 404")
    void delete_spaceNotFound_throws() {
        User host = saveUser("host-5");

        assertThatThrownBy(() -> spaceService.deleteSpace(host.getUserId(), 999_999L))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.SPACE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인이 등록한 공간이 아니면 403")
    void delete_notOwner_throws() {
        User host = saveUser("host-6");
        Space space = saveSpace(host.getUserId());

        assertThatThrownBy(() -> spaceService.deleteSpace(OTHER_USER_ID, space.getId()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.NOT_SPACE_OWNER);
    }

    // 헬퍼
    private User saveUser(String socialUid) {
        return userRepository.save(User.builder()
                .socialProvider(SocialProvider.GOOGLE)
                .socialUid(socialUid)
                .nickname("테스터")
                .currentMode(UserMode.HOST)
                .build());
    }

    private Space saveSpace(Long hostId) {
        return spaceRepository.save(Space.builder()
                .buildingName("합정 메세나폴리스")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.LARGE_OFFICE)
                .city("서울특별시")
                .district("마포구")
                .dong("합정동")
                .latitude(37.5012)
                .longitude(127.0397)
                .roadAddress("서울특별시 마포구 합정동 130-3")
                .addressDetail("302동 302호")
                .deposit(4_500_000L)
                .pricePerDay(90_000)
                .availableStartDate(LocalDate.of(2026, 6, 1))
                .availableEndDate(LocalDate.of(2026, 12, 31))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(66.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .floorNumber(2)
                .parkingAvailable(true)
                .description("합정 중심지에 위치한 공간입니다.")
                .hostId(hostId)
                .build());
    }

    private void saveReservation(Space space, User guest, ReservationStatus status) {
        reservationRepository.save(Reservation.builder()
                .status(status)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 5))
                .usagePurpose("테스트 예약")
                .rentalFee(400_000L)
                .deposit(4_500_000L)
                .insuranceFee(20_000L)
                .platformFee(40_000L)
                .totalPrice(4_960_000L)
                .space(space)
                .user(guest)
                .build());
    }
}