package com.popIt.pop_it.domain.wishlist.controller;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.enums.*;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserWishlistControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SpaceRepository spaceRepository;

    @Autowired
    SpaceImageRepository spaceImageRepository;

    @Autowired
    WishlistRepository wishlistRepository;

    @Autowired
    JwtUtil jwtUtil;

    private User user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .socialProvider(SocialProvider.GOOGLE)
                .socialUid("google-uid-mywish-1")
                .nickname("mywish-tester")
                .currentMode(UserMode.GUEST)
                .build());

        accessToken = "Bearer " + jwtUtil.createAccessToken(new AuthUser(user));
    }

    private Space saveSpace(String buildingName, LocalDateTime deletedAt) {
        return spaceRepository.save(Space.builder()
                .buildingName(buildingName)
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울")
                .district("강남구")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("서울특별시 강남구 역삼동 130-3")
                .addressDetail("3층 301호")
                .deposit(1_000_000L)
                .pricePerDay(70_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("찜 목록 조회 테스트용 공간")
                .hostId(user.getUserId())
                .deletedAt(deletedAt)
                .build());
    }

    private void wish(Long userId, Long spaceId) {
        wishlistRepository.saveAndFlush(Wishlist.builder()
                .userId(userId)
                .spaceId(spaceId)
                .build());
    }

    @Test
    @DisplayName("찜한 공간이 있으면 목록과 명세 필드들을 반환한다")
    void getMyWishlist_success() throws Exception {
        Space space = saveSpace("강남 OOO 건물", null);
        spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .imageUrl("https://s3.amazonaws.com/popIt/img1.jpg")
                .sortOrder(0)
                .space(space)
                .build());
        wish(user.getUserId(), space.getId());

        mockMvc.perform(get("/api/v1/users/me/wishlist")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("WISH200_3"))
                .andExpect(jsonPath("$.result.wishlist[0].spaceId").value(space.getId()))
                .andExpect(jsonPath("$.result.wishlist[0].buildingName").value("강남 OOO 건물"))
                .andExpect(jsonPath("$.result.wishlist[0].roadAddress").value("서울특별시 강남구 역삼동 130-3"))
                .andExpect(jsonPath("$.result.wishlist[0].basicInfo").value("POPUP_STORE"))
                .andExpect(jsonPath("$.result.wishlist[0].pricePerDay").value(70_000))
                .andExpect(jsonPath("$.result.wishlist[0].pricePerWeek").doesNotExist())   // null → 직렬화 시 미노출
                .andExpect(jsonPath("$.result.wishlist[0].pricePerMonth").doesNotExist())
                .andExpect(jsonPath("$.result.wishlist[0].thumbnailUrl").value("https://s3.amazonaws.com/popIt/img1.jpg"))
                .andExpect(jsonPath("$.result.wishlist[0].wishCount").value(1))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    @DisplayName("찜한 공간이 없으면 빈 목록과 hasNext=false를 반환한다")
    void getMyWishlist_empty() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/wishlist")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WISH200_3"))
                .andExpect(jsonPath("$.result.wishlist").isEmpty())
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    @DisplayName("삭제된(soft delete) 공간은 찜했더라도 목록에서 제외된다")
    void getMyWishlist_excludesDeletedSpace() throws Exception {
        Space alive = saveSpace("살아있는 공간", null);
        Space deleted = saveSpace("삭제된 공간", LocalDateTime.now());
        wish(user.getUserId(), alive.getId());
        wish(user.getUserId(), deleted.getId());

        mockMvc.perform(get("/api/v1/users/me/wishlist")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.wishlist.length()").value(1))
                .andExpect(jsonPath("$.result.wishlist[0].spaceId").value(alive.getId()));
    }

    @Test
    @DisplayName("최근 찜한 순으로 정렬되고, 페이징(size)과 hasNext가 동작한다")
    void getMyWishlist_pagingAndOrder() throws Exception {
        Space first = saveSpace("먼저 찜", null);
        Space second = saveSpace("나중 찜", null);
        wish(user.getUserId(), first.getId());
        wish(user.getUserId(), second.getId());

        // size=1 → 최근 찜한 second가 먼저, 다음 페이지 존재
        mockMvc.perform(get("/api/v1/users/me/wishlist")
                        .header("Authorization", accessToken)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.wishlist.length()").value(1))
                .andExpect(jsonPath("$.result.wishlist[0].spaceId").value(second.getId()))
                .andExpect(jsonPath("$.result.hasNext").value(true));
    }

    @Test
    @DisplayName("다른 사용자의 찜은 내 목록에 나오지 않는다")
    void getMyWishlist_isolatedPerUser() throws Exception {
        User other = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("other-uid")
                .nickname("other")
                .currentMode(UserMode.GUEST)
                .build());
        Space space = saveSpace("남의 찜 공간", null);
        wish(other.getUserId(), space.getId()); // 다른 사용자가 찜

        mockMvc.perform(get("/api/v1/users/me/wishlist")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.wishlist").isEmpty());
    }

    @Test
    @DisplayName("인증 토큰이 없으면 401을 반환한다")
    void getMyWishlist_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/wishlist"))
                .andExpect(status().isUnauthorized());
    }
}
