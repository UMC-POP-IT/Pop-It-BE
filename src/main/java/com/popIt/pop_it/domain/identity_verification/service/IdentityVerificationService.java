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
import com.popIt.pop_it.global.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityVerificationService {

    private final RestClient portoneRestClient;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final ObjectMapper objectMapper;

    // 본인인증 확인 로직
    public IdentityVerificationResDTO.IdentityVerificationVerifyRes verify(User user, IdentityVerificationReqDTO.IdentityVerificationVerifyReq dto) {
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

        // 인증회원 정보 꺼내기
        IdentityVerificationResDTO.PortOneSuccessRes.VerifiedCustomer customer = response.verifiedCustomer();
        // ciHash 생성
        String ciHash = HashUtil.sha256(customer.ci());

        // 예외 처리 - 이미 존재하는 IdentityVerificationId 로 다시 요청하는 경우
        if (identityVerificationRepository.existsByIdentityVerificationId(dto.identityVerificationId())) {
            throw new IdentityVerificationException(IdentityVerificationErrorCode.ALREADY_PROCESSED);
        }

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
                .verifiedAt(LocalDateTime.ofInstant(response.verifiedAt(), ZoneId.of("Asia/Seoul")))
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

    // Contract에서 본인인증 여부 조회 시 필요
    public Optional<String> getVerifiedCiHash(User user) {
        return identityVerificationRepository.findCiHashByUser(user);
    }
}
