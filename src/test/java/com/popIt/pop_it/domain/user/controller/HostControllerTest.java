package com.popIt.pop_it.domain.user.controller;

import tools.jackson.databind.ObjectMapper;
import com.popIt.pop_it.domain.user.dto.HostRegisterRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HostControllerTest {

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
                .socialUid("google-uid-123")
                .nickname("tester")
                .currentMode(UserMode.GUEST)
                .build());

        accessToken = "Bearer " + jwtUtil.createAccessToken(new AuthUser(user));
    }

    private HostRegisterRequest validRequest() {
        return new HostRegisterRequest(
                TaxationType.SIMPLIFIED,
                "123-45-67890",
                "https://s3.example.com/business-license.png",
                "팝잇 상회",
                "서울시 강남구 테헤란로 1 3층 301호",
                Bank.KB,
                "1234567890",
                "홍길동",
                "https://s3.example.com/bankbook.png"
        );
    }

    @Test
    @DisplayName("호스트 등록 성공 시 201과 생성 정보를 반환하고 사용자 모드를 HOST로 전환한다")
    void registerHost_success() throws Exception {
        mockMvc.perform(post("/api/v1/hosts")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("HOST201_1"))
                .andExpect(jsonPath("$.result.id").isNumber())
                .andExpect(jsonPath("$.result.createdAt").exists());

        assertThat(hostProfileRepository.existsByUserId(user.getUserId())).isTrue();
        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getCurrentMode())
                .isEqualTo(UserMode.HOST);

        // 사업자등록번호는 하이픈 제거 후 숫자 10자리로 저장된다
        HostProfile saved = hostProfileRepository.findAll().get(0);
        assertThat(saved.getBusinessRegistrationNumber()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("이미 호스트 프로필이 존재하면 409를 반환한다")
    void registerHost_conflict() throws Exception {
        hostProfileRepository.save(HostProfile.builder()
                .userId(user.getUserId())
                .taxationType(TaxationType.GENERAL)
                .businessRegistrationNumber("9999999999")
                .businessLicenseUrl("https://s3.example.com/x.png")
                .businessName("기존 상회")
                .businessAddress("서울시 어딘가")
                .bank(Bank.NH)
                .settlementAccountNumber("111222333")
                .accountHolder("김철수")
                .bankbookCopyUrl("https://s3.example.com/y.png")
                .build());

        mockMvc.perform(post("/api/v1/hosts")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("HOST409_1"));
    }

    @Test
    @DisplayName("검증 실패(사업자등록번호 형식 오류) 시 400을 반환한다")
    void registerHost_validationFail() throws Exception {
        HostRegisterRequest invalid = new HostRegisterRequest(
                TaxationType.SIMPLIFIED,
                "123",                         // 형식 오류: 10자리가 아님
                "https://s3.example.com/a.png",
                "팝잇 상회",
                "서울시 강남구",
                Bank.KB,
                "1234567890",
                "홍길동",
                "https://s3.example.com/b.png"
        );

        mockMvc.perform(post("/api/v1/hosts")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        assertThat(hostProfileRepository.existsByUserId(user.getUserId())).isFalse();
    }

    @Test
    @DisplayName("인증 토큰이 없으면 401을 반환한다")
    void registerHost_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/hosts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }
}
