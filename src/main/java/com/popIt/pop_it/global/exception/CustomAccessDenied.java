package com.popIt.pop_it.global.exception;

import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class CustomAccessDenied implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        BaseErrorCode code = GeneralErrorCode.FORBIDDEN;

        // 응답 Content-type, HTTP 상태 코드 정의
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code.getStatus().value());

        // Response Body에 응답 통일한 객체를 넣기
        ApiResponse<Void> errorResponse = ApiResponse.onFailure(code, null);

        // 실제 Response로 덮어쓰기
        objectMapper.writeValue(response.getOutputStream(), errorResponse);

    }
}
