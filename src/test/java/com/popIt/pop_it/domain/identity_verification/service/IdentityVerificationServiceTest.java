package com.popIt.pop_it.domain.identity_verification.service;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationReqDTO;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.identity_verification.entity.enums.Gender;
import com.popIt.pop_it.domain.identity_verification.entity.enums.PortOneVerificationStatus;
import com.popIt.pop_it.domain.identity_verification.repository.IdentityVerificationRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.util.CryptoService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 본인인증 여부 조회(isVerified)와 본인인증(verify)의 정상 케이스를 검증한다. 실패 케이스는 IdentityVerificationServiceFailureTest 참고.
// 실제 포트원 서버를 호출하지 않도록, portoneRestClient 빈을 MockRestServiceServer로 바인딩한 것으로 교체한다.
@SpringBootTest
@Transactional
public class IdentityVerificationServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IdentityVerificationRepository identityVerificationRepository;
    @Autowired
    private IdentityVerificationService identityVerificationService;
    @Autowired
    private CryptoService cryptoService;
    @Autowired
    private MockRestServiceServer mockPortoneServer;

    private static final String PORTONE_BASE_URL = "https://api.portone.io";

    @TestConfiguration
    static class PortoneRestClientTestConfig {

        @Bean
        RestClient.Builder mockPortoneRestClientBuilder() {
            return RestClient.builder().baseUrl(PORTONE_BASE_URL);
        }

        @Bean
        MockRestServiceServer mockPortoneServer(RestClient.Builder mockPortoneRestClientBuilder) {
            return MockRestServiceServer.bindTo(mockPortoneRestClientBuilder).build();
        }

        // 이름을 다르게 두고 @Primary로만 우선권을 줘서, RestClientConfig의 실제 portoneRestClient 빈 정의와
        // 이름이 충돌(BeanDefinitionOverrideException)하지 않게 한다.
        @Bean
        @Primary
        RestClient testPortoneRestClient(RestClient.Builder mockPortoneRestClientBuilder, MockRestServiceServer mockPortoneServer) {
            return mockPortoneRestClientBuilder.build();
        }
    }

    private User host;

    @BeforeEach
    void setUp() {
        mockPortoneServer.reset();

        host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("identity-verification-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
    }

    /**
     * isVerified() - 본인인증 여부 조회
     */

    @Test
    void 본인인증_이력이_없으면_미인증으로_조회된다() {
        IdentityVerificationResDTO.IdentityVerificationVerifyRes result = identityVerificationService.isVerified(host);

        assertThat(result.isVerified()).isFalse();
        assertThat(result.verifiedAt()).isNull();
    }

    @Test
    void 본인인증이_완료된_사용자는_인증완료로_조회된다() {
        LocalDateTime verifiedAt = LocalDateTime.now().minusDays(1);
        saveVerifiedIdentity(host, "already-verified-id", "already-verified-ci", verifiedAt);

        IdentityVerificationResDTO.IdentityVerificationVerifyRes result = identityVerificationService.isVerified(host);

        assertThat(result.isVerified()).isTrue();
        assertThat(result.verifiedAt()).isEqualTo(verifiedAt);
    }

    /**
     * verify() - 포트원 연동 본인인증
     */

    @Test
    void 정상적으로_본인인증에_성공하면_인증정보가_저장된다() {
        String identityVerificationId = "verify-success-id";
        mockPortoneServer.expect(requestTo(PORTONE_BASE_URL + "/identity-verifications/" + identityVerificationId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(portoneVerifiedJson(identityVerificationId, "success-ci-value"), MediaType.APPLICATION_JSON));

        IdentityVerificationResDTO.IdentityVerificationVerifyRes result = identityVerificationService.verify(
                host, new IdentityVerificationReqDTO.IdentityVerificationVerifyReq(identityVerificationId));

        assertThat(result.isVerified()).isTrue();
        assertThat(result.verifiedAt()).isNotNull();

        IdentityVerification saved = identityVerificationRepository.findByUser(host).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PortOneVerificationStatus.VERIFIED);
        assertThat(saved.getCiHash()).isEqualTo(cryptoService.hash("success-ci-value"));
        assertThat(saved.getCi()).isEqualTo("success-ci-value"); // @Convert가 조회 시 자동 복호화

        mockPortoneServer.verify();
    }

    /**
     * 헬퍼
     */

    private void saveVerifiedIdentity(User user, String identityVerificationId, String ci, LocalDateTime verifiedAt) {
        identityVerificationRepository.save(IdentityVerification.builder()
                .identityVerificationId(identityVerificationId)
                .status(PortOneVerificationStatus.VERIFIED)
                .name("홍길동")
                .gender(Gender.MALE)
                .phone("01012345678")
                .birthDate("1990-01-01")
                .ci(ci)
                .ciHash(cryptoService.hash(ci))
                .user(user)
                .verifiedAt(verifiedAt)
                .build());
    }

    private String portoneVerifiedJson(String identityVerificationId, String ci) {
        return """
                {
                  "id": "%s",
                  "status": "VERIFIED",
                  "verifiedAt": "2026-08-01T10:15:30Z",
                  "verifiedCustomer": {
                    "name": "홍길동",
                    "phoneNumber": "01012345678",
                    "birthDate": "1990-01-01",
                    "gender": "MALE",
                    "ci": "%s"
                  }
                }
                """.formatted(identityVerificationId, ci);
    }
}
