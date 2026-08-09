package com.popIt.pop_it.domain.reservation.controller;

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
import com.popIt.pop_it.global.security.entity.AuthUser;
import com.popIt.pop_it.global.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /api/v1/reservations/unavailable-dates — 경로변수(spaceId)를 쿼리파라미터로 옮긴 것에 대한 MVC 레벨 커버리지
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SpaceRepository spaceRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    JwtUtil jwtUtil;

    private Long hostId;
    private Long spaceId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        User host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("unavailable-dates-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
        hostId = host.getUserId();

        User guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.GOOGLE)
                .socialUid("unavailable-dates-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());

        Space space = spaceRepository.save(Space.builder()
                .buildingName("예약 불가 날짜 테스트용 빌딩")
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
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("예약 불가 날짜 조회 테스트용 공간")
                .hostId(hostId)
                .build());
        spaceId = space.getId();

        reservationRepository.save(Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .usagePurpose("선점 테스트")
                .rentalFee(200_000L)
                .deposit(1_000_000L)
                .insuranceFee(10_000L)
                .platformFee(20_000L)
                .totalPrice(1_210_000L)
                .checkoutRejected(false)
                .space(space)
                .user(guest)
                .build());

        accessToken = "Bearer " + jwtUtil.createAccessToken(new AuthUser(guest));
    }

    @Test
    @DisplayName("정상 조회 시 선점된 기간 목록을 반환한다")
    void getUnavailableDates_success() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/unavailable-dates")
                        .param("spaceId", String.valueOf(spaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("RESERVATION200_8"))
                .andExpect(jsonPath("$.result.unavailableDates.length()").value(1))
                .andExpect(jsonPath("$.result.unavailableDates[0].startDate")
                        .value(LocalDate.now().plusDays(10).toString()))
                .andExpect(jsonPath("$.result.unavailableDates[0].endDate")
                        .value(LocalDate.now().plusDays(12).toString()));
    }

    @Test
    @DisplayName("비로그인 상태로도 조회 가능하다 (인증 없이 허용된 경로)")
    void getUnavailableDates_noAuth_success() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/unavailable-dates")
                        .param("spaceId", String.valueOf(spaceId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("spaceId를 안 보내면 400을 반환한다")
    void getUnavailableDates_missingSpaceId() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/unavailable-dates"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    @DisplayName("spaceId가 숫자가 아니면 400을 반환한다")
    void getUnavailableDates_malformedSpaceId() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/unavailable-dates")
                        .param("spaceId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 spaceId면 404를 반환한다")
    void getUnavailableDates_spaceNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/unavailable-dates")
                        .param("spaceId", "999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SPACE404_1"));
    }

    @Test
    @DisplayName("spaceId가 0 이하(음수/0)면 400을 반환한다")
    void getUnavailableDates_nonPositiveSpaceId() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/unavailable-dates")
                        .param("spaceId", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reservations/unavailable-dates")
                        .param("spaceId", "0"))
                .andExpect(status().isBadRequest());
    }
}
