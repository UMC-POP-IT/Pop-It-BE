package com.popIt.pop_it.global.security.service;

import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.security.entity.OAuthUser;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Provider에서 내려준 사용자 원본 속성 조회
        OAuth2User oauth2User = fetchOAuth2User(userRequest);

        // Provider별 키 규칙에 맞춰 식별자/닉네임 정규화
        SocialProvider socialProvider = extractSocialProvider(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();
        String socialUid = extractSocialUid(socialProvider, attributes);
        String nickname = extractNickname(socialProvider, socialUid, attributes);

        // 기존 회원 재사용, 없으면 가입
        User user = findOrCreateSocialUser(socialProvider, socialUid, nickname);

        return new OAuthUser(user, attributes);
    }

    protected OAuth2User fetchOAuth2User(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    private SocialProvider extractSocialProvider(OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        try {
            return SocialProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 소셜 로그인 제공자입니다: " + registrationId
            );
        }
    }

    private String extractSocialUid(SocialProvider socialProvider, Map<String, Object> attributes) {
        String uid = switch (socialProvider) {
            case GOOGLE -> asString(attributes.get("sub"));
            case KAKAO -> extractKakaoUid(attributes);
        };

        if (uid == null || uid.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_uid"),
                    "소셜 사용자 식별자를 확인할 수 없습니다."
            );
        }
        return uid;
    }

    private String extractNickname(SocialProvider socialProvider, String socialUid, Map<String, Object> attributes) {
        String defaultNickname = socialProvider.name().toLowerCase() + "_" + socialUid;
        String rawNickname = switch (socialProvider) {
            case GOOGLE -> asString(attributes.get("name"));
            case KAKAO -> extractKakaoNickname(attributes);
        };
        String nickname = (rawNickname == null || rawNickname.isBlank()) ? defaultNickname : rawNickname.trim();
        return nickname.length() > User.MAX_NICKNAME_LENGTH
                ? nickname.substring(0, User.MAX_NICKNAME_LENGTH)
                : nickname;
    }

    private User findOrCreateSocialUser(SocialProvider socialProvider, String socialUid, String nickname) {
        Optional<User> existing = userRepository.findBySocialProviderAndSocialUid(socialProvider, socialUid);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return userRepository.save(
                    User.builder()
                            .socialProvider(socialProvider)
                            .socialUid(socialUid)
                            .nickname(nickname)
                            .currentMode(UserMode.GUEST)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 동시 로그인으로 중복 insert 충돌 시, 이미 생성된 사용자 재조회
            return userRepository.findBySocialProviderAndSocialUid(socialProvider, socialUid)
                    .orElseThrow(() -> e);
        }
    }

    private String extractKakaoUid(Map<String, Object> attributes) {
        // 1순위: 평탄화된 응답(id)
        String topLevelId = asString(attributes.get("id"));
        if (topLevelId != null && !topLevelId.isBlank()) {
            return topLevelId;
        }

        // 2순위: 중첩 응답(kakao_account.id)
        Map<String, Object> kakaoAccount = asMap(attributes.get("kakao_account"));
        if (kakaoAccount == null) {
            return null;
        }

        return asString(kakaoAccount.get("id"));
    }

    private String extractKakaoNickname(Map<String, Object> attributes) {
        // 1순위: properties.nickname
        Map<String, Object> properties = asMap(attributes.get("properties"));
        String propertiesNickname = properties == null ? null : asString(properties.get("nickname"));
        if (propertiesNickname != null && !propertiesNickname.isBlank()) {
            return propertiesNickname;
        }

        // 2순위: kakao_account.profile.nickname
        Map<String, Object> kakaoAccount = asMap(attributes.get("kakao_account"));
        if (kakaoAccount == null) {
            return null;
        }

        Map<String, Object> profile = asMap(kakaoAccount.get("profile"));
        if (profile == null) {
            return null;
        }

        return asString(profile.get("nickname"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
