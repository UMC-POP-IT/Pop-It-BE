package com.popIt.pop_it.domain.auth.service;

import com.popIt.pop_it.domain.user.converter.UserConverter;
import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.security.entity.AuthUser;
import com.popIt.pop_it.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Transactional
    public void logout(AuthUser authUser) {
        if (authUser == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        User user = authUser.getUser();
        user.clearRefreshToken();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResDTO.TokenReissueRes reissue(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        String uid = jwtUtil.getUid(refreshToken);
        SocialProvider socialProvider = jwtUtil.getSocialProvider(refreshToken);
        if (uid == null || socialProvider == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findBySocialProviderAndSocialUid(socialProvider, uid)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        String accessToken = jwtUtil.createAccessToken(new AuthUser(user));
        return UserConverter.toReissue(accessToken);
    }
}
