package com.popIt.pop_it.domain.user.service;

import com.popIt.pop_it.domain.user.converter.UserConverter;
import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
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
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final HostProfileRepository hostProfileRepository;

    @Override
    @Transactional
    public UserResDTO.UserInfoRes switchMode(Long userId, UserMode mode) {
        // 인증 주체는 존재해야 정상이나, 모드 전환을 위해 명시적으로 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));

        // 한 사용자당 호스트 프로필은 1개 → 이미 존재하는 선검사 메서드 재사용
        boolean isHost = hostProfileRepository.existsByUserId(userId);

        // HOST 전환은 호스트 프로필이 있어야만 허용, GUEST 전환은 무조건 허용.
        // 이미 같은 모드여도 같은 값 재대입일 뿐이므로 예외 없이 멱등하게 동작한다.
        if (mode == UserMode.HOST) {
            if (!isHost) {
                throw new ProjectException(HostErrorCode.NOT_HOST);
            }
            user.switchToHost();
        } else {
            user.switchToGuest();
        }

        return UserConverter.toUserInfo(user, isHost);
    }
}
