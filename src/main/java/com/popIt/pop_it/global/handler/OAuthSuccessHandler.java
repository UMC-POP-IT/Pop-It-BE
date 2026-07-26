package com.popIt.pop_it.global.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popIt.pop_it.domain.user.converter.UserConverter;
import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.domain.user.exception.code.UserSuccessCode;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.security.entity.AuthUser;
import com.popIt.pop_it.global.security.entity.OAuthUser;
import com.popIt.pop_it.global.security.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // 사전 작업: Response 매핑할 ObjectMapper 선언
        ObjectMapper objectMapper = new ObjectMapper();
        BaseSuccessCode code = UserSuccessCode.USER_LOGIN;

        // Content-Type, Status 설정
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code.getStatus().value());

        // 인증 객체 컨테이너에서 OAuth 인증 객체 가져오기
        OAuthUser user = (OAuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User domainUser = user.getUser();

        // 토큰 제작을 위해 OAuth 인증 객체에서 User 추출 -> AuthUser 제작
        String accessToken = jwtUtil.createAccessToken(new AuthUser(domainUser));
        String refreshToken = jwtUtil.createRefreshToken(new AuthUser(domainUser));
        domainUser.updateRefreshToken(refreshToken);
        userRepository.save(domainUser);

        // 응답 통일 객체 래핑
        ApiResponse<UserResDTO.UserLoginRes> responseBody = ApiResponse.onSuccess(
                code,
                UserConverter.toLogin(accessToken, refreshToken)
        );

        // 응답 출력
        objectMapper.writeValue(response.getOutputStream(), responseBody);

    }
}
