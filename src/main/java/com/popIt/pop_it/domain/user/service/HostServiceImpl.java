package com.popIt.pop_it.domain.user.service;

import com.popIt.pop_it.domain.user.converter.HostConverter;
import com.popIt.pop_it.domain.user.dto.HostRegisterReq;
import com.popIt.pop_it.domain.user.dto.HostRegisterRes;
import com.popIt.pop_it.domain.user.entity.HostProfile;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.exception.code.HostErrorCode;
import com.popIt.pop_it.domain.user.repository.HostProfileRepository;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.util.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HostServiceImpl implements HostService {

    private final HostProfileRepository hostProfileRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    @Override
    @Transactional
    public HostRegisterRes register(Long userId, HostRegisterReq request) {
        // 한 사용자당 호스트 프로필은 1개만 허용 → 중복 등록 선검사(빠른 실패 + 친절한 응답)
        if (hostProfileRepository.existsByUserId(userId)) {
            throw new ProjectException(HostErrorCode.HOST_PROFILE_ALREADY_EXISTS);
        }

        // 사업자등록번호 중복 선검사(암호문은 비교 불가 → 결정적 해시로 판별)
        String businessNumberHash = cryptoService.hash(request.normalizedBusinessRegistrationNumber());
        if (hostProfileRepository.existsByBusinessRegistrationNumberHash(businessNumberHash)) {
            throw new ProjectException(HostErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
        }

        // 인증 주체는 존재해야 정상이나, 모드 전환을 위해 명시적으로 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));

        HostProfile hostProfile = HostConverter.toHostProfile(userId, request, businessNumberHash);
        try {
            // 동시 요청으로 선검사를 함께 통과한 경우, DB 유니크 제약(user_id, 사업자번호 해시)이 최종 방어선
            hostProfileRepository.saveAndFlush(hostProfile);
        } catch (DataIntegrityViolationException e) {
            throw new ProjectException(HostErrorCode.HOST_PROFILE_ALREADY_EXISTS);
        }

        // 등록 완료 → 호스트 권한 활성화 (도메인 메서드로 상태 전이)
        user.switchToHost();

        return HostConverter.toRegisterResponse(hostProfile);
    }
}
