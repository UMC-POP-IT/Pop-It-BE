package com.popIt.pop_it.global.exception;

import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class CustomEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        BaseErrorCode code = GeneralErrorCode.UNAUTHORIZED;

        // 응답 Content-type, HTTP 상태 코드 정의
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code.getStatus().value());

        // Response Body에 응답 통일한 객체를 넣기
        ApiResponse<Void> errorResponse = ApiResponse.onFailure(code, null);

        // 실제 Response로 덮어쓰기
        objectMapper.writeValue(response.getOutputStream(), errorResponse);

    }
}
