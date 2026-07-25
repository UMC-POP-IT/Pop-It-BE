package com.popIt.pop_it.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 프론트가 소셜 로그인을 시작할 때 함께 넘긴 PKCE challenge 값을
 * (예: GET /api/v1/auth/oauth/kakao?challenge=xxx) HttpSession에 저장해두는 필터.
 * 이 세션은 카카오/구글 인가 플로우 동안(oauthSecurityFilterChain, IF_REQUIRED)에만 쓰이고,
 * 콜백에서 OAuthSuccessHandler가 challenge를 꺼내 발급 코드에 바인딩한다.
 */
public class OAuthChallengeCaptureFilter extends OncePerRequestFilter {

    public static final String CHALLENGE_SESSION_KEY = "oauth_challenge";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        // 로그인 "시작" 요청에서만 캡처 (콜백 경로는 제외)
        if (uri.startsWith("/api/v1/auth/oauth/") && !uri.contains("/callback/")) {
            String challenge = request.getParameter("challenge");
            if (challenge != null && !challenge.isBlank()) {
                HttpSession session = request.getSession(true);
                session.setAttribute(CHALLENGE_SESSION_KEY, challenge);
            }
        }
        filterChain.doFilter(request, response);
    }
}
