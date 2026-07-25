package com.popIt.pop_it.global.handler;

import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.security.entity.AuthUser;
import com.popIt.pop_it.global.security.entity.OAuthUser;
import com.popIt.pop_it.global.security.filter.OAuthChallengeCaptureFilter;
import com.popIt.pop_it.global.security.oauth.OAuthCodeStore;
import com.popIt.pop_it.global.security.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OAuthCodeStore oAuthCodeStore;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // 인증 객체 컨테이너에서 OAuth 인증 객체 가져오기
        OAuthUser user = (OAuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User domainUser = user.getUser();

        // 토큰 제작을 위해 OAuth 인증 객체에서 User 추출 -> AuthUser 제작
        String accessToken = jwtUtil.createAccessToken(new AuthUser(domainUser));
        String refreshToken = jwtUtil.createRefreshToken(new AuthUser(domainUser));
        domainUser.updateRefreshToken(refreshToken);
        userRepository.save(domainUser);

        // 로그인 시작 시 OAuthChallengeCaptureFilter가 세션에 저장해둔 challenge를 꺼내
        // 발급 코드에 바인딩 -> exchange 시 verifier 검증 없이는 code만으로 토큰 교환 불가
        HttpSession session = request.getSession(false);
        String challenge = session != null
                ? (String) session.getAttribute(OAuthChallengeCaptureFilter.CHALLENGE_SESSION_KEY)
                : null;

        String code = oAuthCodeStore.issue(accessToken, refreshToken, challenge);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("code", code)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}

