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

    // 로그인을 시작한 프론트 origin. 콜백에서 OAuthSuccessHandler가 꺼내
    // 화이트리스트 검증 후 그 origin으로 복귀시킨다 (verifier가 시작 origin에만 저장되므로).
    public static final String REDIRECT_ORIGIN_SESSION_KEY = "oauth_redirect_origin";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        // 로그인 "시작" 요청에서만 처리 (콜백 경로는 제외)
        if (uri.startsWith("/api/v1/auth/oauth/") && !uri.contains("/callback/")) {
            HttpSession session = request.getSession(true);
            // 이전 로그인 시도의 challenge/origin이 남아있다가 이번 시도에 잘못 재사용되는 것을 방지
            session.removeAttribute(CHALLENGE_SESSION_KEY);
            session.removeAttribute(REDIRECT_ORIGIN_SESSION_KEY);

            String challenge = request.getParameter("challenge");
            if (challenge != null && !challenge.isBlank()) {
                session.setAttribute(CHALLENGE_SESSION_KEY, challenge);
            }

            // 시작 origin을 저장만 해둔다. 실제 허용 여부(화이트리스트 검증)는 목적지를 정하는 시점에 판단한다.
            String origin = request.getParameter("origin");
            if (origin != null && !origin.isBlank()) {
                session.setAttribute(REDIRECT_ORIGIN_SESSION_KEY, origin);
            }
        }
        filterChain.doFilter(request, response);
    }
}
