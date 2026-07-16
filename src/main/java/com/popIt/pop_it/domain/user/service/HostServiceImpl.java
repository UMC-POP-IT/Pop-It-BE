package com.popIt.pop_it.domain.user.service;

import com.popIt.pop_it.domain.user.converter.HostConverter;
import com.popIt.pop_it.domain.user.dto.HostRegisterRequest;
import com.popIt.pop_it.domain.user.dto.HostRegisterResponse;
import com.popIt.pop_it.domain.user.entity.HostProfile;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.exception.code.HostErrorCode;
import com.popIt.pop_it.domain.user.repository.HostProfileRepository;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HostServiceImpl implements HostService {

    private final HostProfileRepository hostProfileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public HostRegisterResponse register(Long userId, HostRegisterRequest request) {
        // 한 사용자당 호스트 프로필은 1개만 허용 → 중복 등록 방지
        if (hostProfileRepository.existsByUserId(userId)) {
            throw new ProjectException(HostErrorCode.HOST_PROFILE_ALREADY_EXISTS);
        }

        // 인증 주체는 존재해야 정상이나, 모드 전환을 위해 명시적으로 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));

        HostProfile hostProfile = HostConverter.toHostProfile(userId, request);
        hostProfileRepository.save(hostProfile);

        // 등록 완료 → 호스트 권한 활성화 (도메인 메서드로 상태 전이)
        user.switchToHost();

        return HostConverter.toRegisterResponse(hostProfile);
    }
}
