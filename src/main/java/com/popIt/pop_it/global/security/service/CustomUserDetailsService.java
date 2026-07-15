package com.popIt.pop_it.global.security.service;


import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.security.entity.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private final UserRepository userRepository;

    public UserDetails loadUserByUidAndSocialType(SocialProvider socialProvider, String uid) {
        // JWT에 담긴 provider + uid 조합으로 사용자 조회
        User user = userRepository.findBySocialProviderAndSocialUid(socialProvider, uid)
                .orElseThrow(() -> new UsernameNotFoundException(
                        // 인증 파이프라인에서 처리 가능한 표준 예외로 전달
                        "소셜 사용자 정보를 찾을 수 없습니다. provider=%s, uid=%s".formatted(socialProvider, uid)
                ));

        // SecurityContext에 들어갈 인증 객체(AuthUser)로 래핑
        return new AuthUser(user);
    }

}
