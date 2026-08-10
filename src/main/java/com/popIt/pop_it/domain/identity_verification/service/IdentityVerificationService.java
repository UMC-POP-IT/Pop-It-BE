package com.popIt.pop_it.domain.identity_verification.service;

import com.popIt.pop_it.domain.identity_verification.converter.IdentityVerificationConverter;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationReqDTO;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.identity_verification.entity.enums.PortOneVerificationStatus;
import com.popIt.pop_it.domain.identity_verification.exception.IdentityVerificationException;
import com.popIt.pop_it.domain.identity_verification.exception.code.IdentityVerificationErrorCode;
import com.popIt.pop_it.domain.identity_verification.repository.IdentityVerificationRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityVerificationService {

    private final RestClient portoneRestClient;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;

    // 본인인증 확인 로직
    public IdentityVerificationResDTO.IdentityVerificationVerifyRes verify(User user, IdentityVerificationReqDTO.IdentityVerificationVerifyReq dto) {

        // 1. 이미 인증 완료(VERIFIED)된 유저 → 즉시 차단 (포트원 호출 전)
        boolean alreadyVerified = identityVerificationRepository.findByUser(user)
                .map(iv -> iv.getStatus() == PortOneVerificationStatus.VERIFIED)
                .orElse(false);
        if (alreadyVerified) {
            throw new IdentityVerificationException(IdentityVerificationErrorCode.ALREADY_VERIFIED_USER);
        }

        // 2. 아직 인증 안 했지만, 같은 시도 ID(ex. 다른 사람이 같은 시도 ID로)를 재요청 → 포트원 호출 전 차단
        // 예외 처리 - 이미 존재하는 IdentityVerificationId 로 다시 요청하는 경우 (더블클릭, 네트워크 재시도)
        if (identityVerificationRepository.existsByIdentityVerificationId(dto.identityVerificationId())) {
            throw new IdentityVerificationException(IdentityVerificationErrorCode.ALREADY_PROCESSED);
        }

        IdentityVerificationResDTO.PortOneSuccessRes response = portoneRestClient.get()
                .uri("/identity-verifications/{id}", dto.identityVerificationId())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());

                    log.error("포트원 API 에러 - status: {}, body: {}", res.getStatusCode(), body);

                    // 포트원 서버 에러
                    IdentityVerificationResDTO.PortOneErrorRes portOneError;
                    try {
                        portOneError = objectMapper.readValue(body, IdentityVerificationResDTO.PortOneErrorRes.class);
                    } catch (Exception e) {
                        log.error("포트원 에러 응답 파싱 실패, 원본 body: {}", body, e);
                        throw new IdentityVerificationException(
                                IdentityVerificationErrorCode.PORTONE_API_ERROR,
                                "포트원 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                        );
                    }

                    // 인증 실패
                    throw new IdentityVerificationException(
                            IdentityVerificationErrorCode.PORTONE_API_ERROR,
                            String.format("포트원 인증 조회 실패 [%s]: %s", portOneError.type(), portOneError.message())
                    );
                })
                .body(IdentityVerificationResDTO.PortOneSuccessRes.class);

        // 정상(2xx) 상태코드인데 바디가 비어있는 경우 방어
        if (response == null) {
            throw new IdentityVerificationException(
                    IdentityVerificationErrorCode.PORTONE_API_ERROR, "포트원 응답이 비어있습니다.");
        }

        if (PortOneVerificationStatus.from(response.status()) != PortOneVerificationStatus.VERIFIED) {
            throw new ProjectException(IdentityVerificationErrorCode.NOT_VERIFIED);
        }

        // VERIFIED인데 필수 필드가 비어있는 경우 방어
        validateVerifiedResponse(response);

        // 인증회원 정보 꺼내기
        IdentityVerificationResDTO.PortOneSuccessRes.VerifiedCustomer customer = response.verifiedCustomer();
        // ciHash 생성 - 비밀키 기반 HMAC을 사용해 원문 역추적을 어렵게 함
        String ciHash = cryptoService.hash(customer.ci());


        // 예외 처리 - 본인인증 건이 이미 존재. 서로 다른 identityVerificationId지만, CI(연계정보)가 동일함
        if (identityVerificationRepository.existsByCiHash(ciHash)) {
            throw new IdentityVerificationException(IdentityVerificationErrorCode.DUPLICATE_IDENTITY);
        }

        // response 를 DB에 저장
        IdentityVerification identityVerification = IdentityVerification.builder()
                .identityVerificationId(response.identityVerificationId())
                .status(PortOneVerificationStatus.from(response.status()))
                .name(customer.name())
                .gender(customer.gender())
                .phone(customer.phoneNumber())
                .birthDate(customer.birthDate())
                .ci(customer.ci()) // 저장 시 @Convert가 자동으로 암호화 (매번 다른 암호문)
                .ciHash(ciHash)  // 검색/중복체크용 해시
                .user(user)
                .verifiedAt(LocalDateTime.ofInstant(response.verifiedAt(), ZoneOffset.UTC))
                .build();


        IdentityVerification saved;
        try {
            saved = identityVerificationRepository.save(identityVerification);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 들어와 유니크 제약을 위반한 경우, 어떤 제약을 위반했는지 판별
            if (identityVerificationRepository.existsByIdentityVerificationId(dto.identityVerificationId())) {
                throw new IdentityVerificationException(IdentityVerificationErrorCode.ALREADY_PROCESSED);
            }
            if (identityVerificationRepository.existsByCiHash(ciHash)) {
                throw new IdentityVerificationException(IdentityVerificationErrorCode.DUPLICATE_IDENTITY);
            }
            throw e;
        }

        // DTO 변환
        return IdentityVerificationConverter.toVerify(saved);

    }

    public IdentityVerificationResDTO.IdentityVerificationVerifyRes isVerified(User user) {

        // userId로 IdentityVerification 조회 - 본인인증 이력이 없으면 null(정상, isVerified=false로 응답)
        IdentityVerification verifiedUser = identityVerificationRepository.findByUser(user).orElse(null);

        return IdentityVerificationConverter.toVerify(verifiedUser);
    }

    /**
     * 기타 함수
     */

    // status가 VERIFIED인데도 실제로 필요한 필드가 비어있는, 계약 위반에 가까운 응답을 방어
    // (identityVerificationId는 이미 앞단에서 경로 변수로 받은 값이라 여기선 응답에 실린 값만 재검증)
    private void validateVerifiedResponse(IdentityVerificationResDTO.PortOneSuccessRes response) {
        IdentityVerificationResDTO.PortOneSuccessRes.VerifiedCustomer customer = response.verifiedCustomer();

        boolean missingRequiredField =
                isBlank(response.identityVerificationId())
                        || response.verifiedAt() == null
                        || customer == null
                        || isBlank(customer.ci())
                        || isBlank(customer.name())
                        || isBlank(customer.phoneNumber())
                        || isBlank(customer.birthDate())
                        || customer.gender() == null;

        if (missingRequiredField) {
            throw new IdentityVerificationException(
                    IdentityVerificationErrorCode.PORTONE_API_ERROR,
                    "포트원 응답에 필수 필드가 누락되어 있습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // Contract에서 본인인증 여부 조회 시 필요
    public Optional<String> getVerifiedCiHash(User user) {
        return identityVerificationRepository.findCiHashByUser(user);
    }
}
