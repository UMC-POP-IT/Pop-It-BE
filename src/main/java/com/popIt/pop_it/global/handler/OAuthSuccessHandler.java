package com.popIt.pop_it.global.handler;

import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.security.entity.AuthUser;
import com.popIt.pop_it.global.security.entity.OAuthUser;
import com.popIt.pop_it.global.security.oauth.OAuthCodeStore;
import com.popIt.pop_it.global.security.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

        // 토큰을 URL에 직접 노출하지 않기 위해 1회용 코드로 교환해서 전달
        // 프론트는 이 code를 /api/v1/auth/exchange 로 보내 실제 토큰을 받는다
        String code = oAuthCodeStore.issue(accessToken, refreshToken);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("code", code)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}

