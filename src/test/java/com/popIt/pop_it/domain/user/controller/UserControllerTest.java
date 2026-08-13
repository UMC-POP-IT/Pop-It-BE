package com.popIt.pop_it.domain.user.controller;

import tools.jackson.databind.ObjectMapper;
import com.popIt.pop_it.domain.user.dto.UserReqDTO;
import com.popIt.pop_it.domain.user.entity.HostProfile;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.Bank;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.TaxationType;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.HostProfileRepository;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.security.entity.AuthUser;
import com.popIt.pop_it.global.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    HostProfileRepository hostProfileRepository;

    @Autowired
    JwtUtil jwtUtil;

    private User user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .socialProvider(SocialProvider.GOOGLE)
                .socialUid("google-uid-mode-123")
                .nickname("tester")
                .currentMode(UserMode.GUEST)
                .build());

        accessToken = "Bearer " + jwtUtil.createAccessToken(new AuthUser(user));
    }

    // 호스트 프로필 저장 (HOST 전환 인가 통과용)
    private void saveHostProfile() {
        hostProfileRepository.save(HostProfile.builder()
                .userId(user.getUserId())
                .taxationType(TaxationType.SIMPLIFIED)
                .businessRegistrationNumber("1234567890")
                .businessRegistrationNumberHash("hash-1234567890")
                .businessLicenseUrl("https://s3.example.com/business-license.png")
                .businessName("팝잇 상회")
                .businessAddress("서울시 강남구 테헤란로 1 3층 301호")
                .bank(Bank.KB)
                .settlementAccountNumber("1234567890")
                .accountHolder("홍길동")
                .bankbookCopyUrl("https://s3.example.com/bankbook.png")
                .build());
    }

    private String body(UserMode mode) throws Exception {
        return objectMapper.writeValueAsString(new UserReqDTO.ModeSwitchReq(mode));
    }

    @Test
    @DisplayName("호스트 프로필이 있으면 HOST로 전환에 성공하고 200과 사용자 정보를 반환한다")
    void switchToHost_success() throws Exception {
        saveHostProfile();

        mockMvc.perform(patch("/api/v1/users/me/mode")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UserMode.HOST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("USER200_5"))
                .andExpect(jsonPath("$.result.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.result.nickname").value("tester"))
                .andExpect(jsonPath("$.result.currentMode").value("HOST"))
                .andExpect(jsonPath("$.result.hasHostProfile").value(true));

        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getCurrentMode())
                .isEqualTo(UserMode.HOST);
    }

    @Test
    @DisplayName("HOST로 전환했다가 다시 GUEST로 전환에 성공한다 (양방향 전환)")
    void switchBackToGuest_success() throws Exception {
        saveHostProfile();
        user.switchToHost();
        userRepository.saveAndFlush(user);

        mockMvc.perform(patch("/api/v1/users/me/mode")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UserMode.GUEST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER200_5"))
                .andExpect(jsonPath("$.result.currentMode").value("GUEST"))
                .andExpect(jsonPath("$.result.hasHostProfile").value(true));

        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getCurrentMode())
                .isEqualTo(UserMode.GUEST);
    }

    @Test
    @DisplayName("호스트 프로필 없이 HOST로 전환하면 403을 반환한다")
    void switchToHost_withoutProfile_forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/mode")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UserMode.HOST)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("HOST403_1"));

        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getCurrentMode())
                .isEqualTo(UserMode.GUEST);
    }

    @Test
    @DisplayName("이미 GUEST인 사용자가 GUEST로 재전환해도 예외 없이 200을 반환한다 (멱등)")
    void switchToGuest_idempotent() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/mode")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UserMode.GUEST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER200_5"))
                .andExpect(jsonPath("$.result.currentMode").value("GUEST"))
                .andExpect(jsonPath("$.result.hasHostProfile").value(false));
    }

    @Test
    @DisplayName("모드 값이 누락되면 400을 반환한다")
    void switchMode_validationFail() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/mode")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    @DisplayName("인증 토큰이 없으면 401을 반환한다")
    void switchMode_unauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UserMode.GUEST)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보 조회 성공 시 200과 4개 필드를 반환하고 민감정보는 노출하지 않는다")
    void getMyInfo_success() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("USER200_4"))
                .andExpect(jsonPath("$.result.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.result.nickname").value("tester"))
                .andExpect(jsonPath("$.result.currentMode").value("GUEST"))
                .andExpect(jsonPath("$.result.hasHostProfile").value(false))
                // 민감정보/죽은 필드는 응답에 절대 포함되지 않아야 한다
                .andExpect(jsonPath("$.result.socialUid").doesNotExist())
                .andExpect(jsonPath("$.result.socialProvider").doesNotExist())
                .andExpect(jsonPath("$.result.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.result.deletedAt").doesNotExist());
    }

    @Test
    @DisplayName("호스트 프로필이 있으면 내 정보 조회 시 hasHostProfile=true를 반환한다")
    void getMyInfo_withHostProfile() throws Exception {
        saveHostProfile();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.hasHostProfile").value(true))
                // 프로필만 있고 아직 모드는 전환하지 않았다면 currentMode는 GUEST 유지
                .andExpect(jsonPath("$.result.currentMode").value("GUEST"));
    }

    @Test
    @DisplayName("내 정보 조회 시 인증 토큰이 없으면 401을 반환한다")
    void getMyInfo_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
