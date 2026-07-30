package com.popIt.pop_it.global.handler;

import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.security.entity.AuthUser;
import com.popIt.pop_it.global.security.entity.OAuthUser;
import com.popIt.pop_it.global.security.filter.OAuthChallengeCaptureFilter;
import com.popIt.pop_it.global.security.oauth.FrontendOriginResolver;
import com.popIt.pop_it.global.security.oauth.OAuthCodeStore;
import com.popIt.pop_it.global.security.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
    private final FrontendOriginResolver frontendOriginResolver;

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
        String challenge = null;
        String requestedOrigin = null;
        if (session != null) {
            challenge = (String) session.getAttribute(OAuthChallengeCaptureFilter.CHALLENGE_SESSION_KEY);
            requestedOrigin = (String) session.getAttribute(OAuthChallengeCaptureFilter.REDIRECT_ORIGIN_SESSION_KEY);
            // 성공 처리 즉시 제거 -> 동일 세션의 다음 로그인 시도에 이번 challenge/origin이 재사용/오염되는 것을 방지
            session.removeAttribute(OAuthChallengeCaptureFilter.CHALLENGE_SESSION_KEY);
            session.removeAttribute(OAuthChallengeCaptureFilter.REDIRECT_ORIGIN_SESSION_KEY);
        }

        // 화이트리스트로 검증된 최종 목적지. 시작 origin이 없거나 허용 목록에 없으면 기본 frontend-url로 폴백한다.
        // 성공/에러 리다이렉트 모두 이 값을 써야, verifier가 저장된 시작 origin으로 일관되게 복귀한다.
        String targetOrigin = frontendOriginResolver.resolve(requestedOrigin);

        // challenge가 없으면 어차피 exchange에서 항상 실패하는 무효한 code이므로,
        // 아예 발급하지 않고 프론트에 에러 상태로 리다이렉트한다 (PKCE 필수 정책)
        if (challenge == null || challenge.isBlank()) {
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(targetOrigin)
                    .queryParam("error", "missing_challenge")
                    .build()
                    .toUriString();
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        String code = oAuthCodeStore.issue(accessToken, refreshToken, challenge);

        String redirectUrl = UriComponentsBuilder.fromUriString(targetOrigin)
                .queryParam("code", code)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}

