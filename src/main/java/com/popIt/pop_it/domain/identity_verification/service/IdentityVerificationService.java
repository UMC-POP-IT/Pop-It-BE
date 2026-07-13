package com.popIt.pop_it.domain.identity_verification.service;

import com.popIt.pop_it.domain.identity_verification.converter.IdentityVerificationConverter;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationReqDTO;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.identity_verification.exception.IdentityVerificationException;
import com.popIt.pop_it.domain.identity_verification.exception.code.IdentityVerificationErrorCode;
import com.popIt.pop_it.domain.identity_verification.repository.IdentityVerificationRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityVerificationService {

    private final RestClient portoneRestClient;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final ObjectMapper objectMapper;

    // 본인인증 확인 로직
    public IdentityVerificationResDTO.Verify verify(User user, IdentityVerificationReqDTO.Verify dto) {
        IdentityVerificationResDTO.PortOneIdentityVerification response = portoneRestClient.get()
                .uri("/identity-verifications/{id}", dto.identityVerificationId())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());

                    log.error("포트원 API 에러 - status: {}, body: {}", res.getStatusCode(), body);

                    IdentityVerificationResDTO.PortOneError portOneError = objectMapper.readValue(body, IdentityVerificationResDTO.PortOneError.class);
                    throw new IdentityVerificationException(
                            IdentityVerificationErrorCode.PORTONE_API_ERROR,
                            String.format("포트원 인증 조회 실패 [%s]: %s", portOneError.type(), portOneError.message())
                    );
                })
                .body(IdentityVerificationResDTO.PortOneIdentityVerification.class);

        if (!"VERIFIED".equals(response.status())) {
            throw new ProjectException(IdentityVerificationErrorCode.NOT_VERIFIED);
        }

        // 전체 response 로그 찍기
        log.info("포트원 API 응답: {}", response);

        // 인증회원 정보 꺼내기
        IdentityVerificationResDTO.PortOneIdentityVerification.VerifiedCustomer customer = response.verifiedCustomer();

        // response 를 DB에 저장
        IdentityVerification identityVerification = IdentityVerification.builder()
                .identityVerificationId(response.identityVerificationId())
                .status(response.status())
                .name(customer.name())
                .gender(customer.gender())
                .phone(customer.phoneNumber())
                .birthDate(customer.birthDate())
                .ci(customer.ci())
                .user(user) // TODO: 로그인 인증 정보로 연결시키기
                .verifiedAt(LocalDateTime.ofInstant(response.verifiedAt(), ZoneId.of("Asia/Seoul")))
                .build();


        IdentityVerification saved = identityVerificationRepository.save(identityVerification);

        // DTO 변환
        return IdentityVerificationConverter.toVerify(saved);

    }
}
