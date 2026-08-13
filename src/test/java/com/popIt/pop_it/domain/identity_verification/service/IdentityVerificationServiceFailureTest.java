package com.popIt.pop_it.domain.identity_verification.service;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationReqDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.identity_verification.entity.enums.Gender;
import com.popIt.pop_it.domain.identity_verification.entity.enums.PortOneVerificationStatus;
import com.popIt.pop_it.domain.identity_verification.exception.IdentityVerificationException;
import com.popIt.pop_it.domain.identity_verification.exception.code.IdentityVerificationErrorCode;
import com.popIt.pop_it.domain.identity_verification.repository.IdentityVerificationRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 본인인증(verify)이 거부되어야 하는 실패 케이스를 검증한다. 정상 케이스 및 isVerified()는 IdentityVerificationServiceTest 참고.
// 실제 포트원 서버를 호출하지 않도록, portoneRestClient 빈을 MockRestServiceServer로 바인딩한 것으로 교체한다.
@SpringBootTest
@Transactional
public class IdentityVerificationServiceFailureTest {

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
    private User guest;

    @BeforeEach
    void setUp() {
        mockPortoneServer.reset();

        host = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("identity-verification-host-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());

        guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("identity-verification-guest-uid")
                .nickname("guest")
                .currentMode(UserMode.GUEST)
                .build());
    }

    @Test
    void 이미_본인인증된_유저가_다시_요청하면_예외가_발생하고_포트원을_호출하지_않는다() {
        saveVerifiedIdentity(host, "already-verified-id", "already-verified-ci", LocalDateTime.now());
        // 포트원 호출 전에 차단돼야 하므로 mockPortoneServer에 어떤 응답도 등록하지 않는다.
        // 코드가 실제로 호출을 시도하면 MockRestServiceServer가 "예상치 못한 요청"으로 즉시 실패시킨다.

        assertThatThrownBy(() -> identityVerificationService.verify(
                host, new IdentityVerificationReqDTO.IdentityVerificationVerifyReq("new-attempt-id")))
                .isInstanceOf(IdentityVerificationException.class)
                .satisfies(e -> assertThat(((IdentityVerificationException) e).getErrorCode())
                        .isEqualTo(IdentityVerificationErrorCode.ALREADY_VERIFIED_USER));
    }

    @Test
    void 이미_처리된_인증시도_id로_재요청하면_예외가_발생하고_포트원을_호출하지_않는다() {
        // 전제: 다른 사용자가 이미 이 identityVerificationId로 인증을 완료해둔 상태 (더블클릭/네트워크 재시도 재현)
        User otherUser = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("identity-verification-other-uid")
                .nickname("other")
                .currentMode(UserMode.GUEST)
                .build());
        saveVerifiedIdentity(otherUser, "reused-attempt-id", "other-ci-value", LocalDateTime.now());

        assertThatThrownBy(() -> identityVerificationService.verify(
                host, new IdentityVerificationReqDTO.IdentityVerificationVerifyReq("reused-attempt-id")))
                .isInstanceOf(IdentityVerificationException.class)
                .satisfies(e -> assertThat(((IdentityVerificationException) e).getErrorCode())
                        .isEqualTo(IdentityVerificationErrorCode.ALREADY_PROCESSED));
    }

    @Test
    void 포트원_응답이_인증완료가_아니면_예외가_발생한다() {
        String identityVerificationId = "verify-failed-id";
        mockPortoneServer.expect(requestTo(PORTONE_BASE_URL + "/identity-verifications/" + identityVerificationId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(portoneNotVerifiedJson(identityVerificationId), MediaType.APPLICATION_JSON));

        // 주의: 이 분기는 다른 곳과 달리 IdentityVerificationException이 아닌 ProjectException을 직접 던진다(현재 구현 기준).
        assertThatThrownBy(() -> identityVerificationService.verify(
                host, new IdentityVerificationReqDTO.IdentityVerificationVerifyReq(identityVerificationId)))
                .isInstanceOf(ProjectException.class)
                .satisfies(e -> assertThat(((ProjectException) e).getErrorCode())
                        .isEqualTo(IdentityVerificationErrorCode.NOT_VERIFIED));

        assertThat(identityVerificationRepository.findByUser(host)).isEmpty();
    }

    @Test
    void 다른_인증시도지만_동일인이면_중복가입_예외가_발생한다() {
        // 전제: 다른 사용자가 이미 같은 CI(연계정보)로 본인인증을 완료해둔 상태
        String duplicateCi = "duplicate-ci-value";
        User otherUser = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("identity-verification-duplicate-owner-uid")
                .nickname("duplicate-owner")
                .currentMode(UserMode.GUEST)
                .build());
        saveVerifiedIdentity(otherUser, "first-attempt-id", duplicateCi, LocalDateTime.now());

        String identityVerificationId = "second-attempt-id";
        mockPortoneServer.expect(requestTo(PORTONE_BASE_URL + "/identity-verifications/" + identityVerificationId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(portoneVerifiedJson(identityVerificationId, duplicateCi), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> identityVerificationService.verify(
                guest, new IdentityVerificationReqDTO.IdentityVerificationVerifyReq(identityVerificationId)))
                .isInstanceOf(IdentityVerificationException.class)
                .satisfies(e -> assertThat(((IdentityVerificationException) e).getErrorCode())
                        .isEqualTo(IdentityVerificationErrorCode.DUPLICATE_IDENTITY));

        assertThat(identityVerificationRepository.findByUser(guest)).isEmpty();
    }

    @Test
    void 포트원_api가_에러를_응답하면_예외가_발생한다() {
        String identityVerificationId = "portone-error-id";
        mockPortoneServer.expect(requestTo(PORTONE_BASE_URL + "/identity-verifications/" + identityVerificationId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "type": "IDENTITY_VERIFICATION_NOT_FOUND",
                                  "message": "본인인증 내역을 찾을 수 없습니다."
                                }
                                """));

        assertThatThrownBy(() -> identityVerificationService.verify(
                host, new IdentityVerificationReqDTO.IdentityVerificationVerifyReq(identityVerificationId)))
                .isInstanceOf(IdentityVerificationException.class)
                .satisfies(e -> assertThat(((IdentityVerificationException) e).getErrorCode())
                        .isEqualTo(IdentityVerificationErrorCode.PORTONE_API_ERROR));
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

    private String portoneNotVerifiedJson(String identityVerificationId) {
        return """
                {
                  "id": "%s",
                  "status": "FAILED",
                  "verifiedAt": "2026-08-01T10:15:30Z"
                }
                """.formatted(identityVerificationId);
    }
}
