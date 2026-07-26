package com.popIt.pop_it.domain.wishlist.controller;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WishlistControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SpaceRepository spaceRepository;

    @Autowired
    WishlistRepository wishlistRepository;

    @Autowired
    JwtUtil jwtUtil;

    private User user;
    private Space space;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .socialProvider(SocialProvider.GOOGLE)
                .socialUid("google-uid-wish-1")
                .nickname("wish-tester")
                .currentMode(UserMode.GUEST)
                .build());

        space = spaceRepository.save(sampleSpace(user.getUserId(), null));

        accessToken = "Bearer " + jwtUtil.createAccessToken(new AuthUser(user));
    }

    // 테스트용 공간 한 건 생성 (deletedAt로 삭제 여부 제어)
    private Space sampleSpace(Long hostId, java.time.LocalDateTime deletedAt) {
        return Space.builder()
                .buildingName("찜 테스트용 빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("3층 301호")
                .deposit(1_000_000L)
                .pricePerDay(100_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("찜 토글 테스트용 공간")
                .hostId(hostId)
                .deletedAt(deletedAt)
                .build();
    }

    @Test
    @DisplayName("찜하지 않은 공간을 토글하면 200과 isWishlisted=true를 반환하고 DB에 저장된다")
    void toggle_add_success() throws Exception {
        mockMvc.perform(post("/api/v1/spaces/{spaceId}/wishlist", space.getId())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("WISH200_1"))
                .andExpect(jsonPath("$.result.spaceId").value(space.getId()))
                .andExpect(jsonPath("$.result.isWishlisted").value(true));

        assertThat(wishlistRepository.existsByUserIdAndSpaceId(user.getUserId(), space.getId())).isTrue();
    }

    @Test
    @DisplayName("이미 찜한 공간을 토글하면 200과 isWishlisted=false를 반환하고 DB에서 삭제된다")
    void toggle_remove_success() throws Exception {
        // 선행: 이미 찜한 상태로 만든다
        wishlistRepository.saveAndFlush(Wishlist.builder()
                .userId(user.getUserId())
                .spaceId(space.getId())
                .build());

        mockMvc.perform(post("/api/v1/spaces/{spaceId}/wishlist", space.getId())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("WISH200_2"))
                .andExpect(jsonPath("$.result.spaceId").value(space.getId()))
                .andExpect(jsonPath("$.result.isWishlisted").value(false));

        assertThat(wishlistRepository.existsByUserIdAndSpaceId(user.getUserId(), space.getId())).isFalse();
    }

    @Test
    @DisplayName("연속 두 번 토글하면 등록 → 해제로 원상복귀한다")
    void toggle_twice_returns_to_origin() throws Exception {
        // 1회차: 등록
        mockMvc.perform(post("/api/v1/spaces/{spaceId}/wishlist", space.getId())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isWishlisted").value(true));

        // 2회차: 해제
        mockMvc.perform(post("/api/v1/spaces/{spaceId}/wishlist", space.getId())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isWishlisted").value(false));

        assertThat(wishlistRepository.existsByUserIdAndSpaceId(user.getUserId(), space.getId())).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 공간을 토글하면 404(SPACE404_1)를 반환한다")
    void toggle_spaceNotFound() throws Exception {
        long notExistSpaceId = 999_999L;

        mockMvc.perform(post("/api/v1/spaces/{spaceId}/wishlist", notExistSpaceId)
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("SPACE404_1"));
    }

    @Test
    @DisplayName("삭제된(soft delete) 공간을 토글하면 404(SPACE404_1)를 반환한다")
    void toggle_deletedSpace() throws Exception {
        Space deleted = spaceRepository.save(sampleSpace(user.getUserId(), java.time.LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/spaces/{spaceId}/wishlist", deleted.getId())
                        .header("Authorization", accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SPACE404_1"));

        assertThat(wishlistRepository.existsByUserIdAndSpaceId(user.getUserId(), deleted.getId())).isFalse();
    }

    @Test
    @DisplayName("인증 토큰이 없으면 401을 반환한다")
    void toggle_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/spaces/{spaceId}/wishlist", space.getId()))
                .andExpect(status().isUnauthorized());
    }
}
