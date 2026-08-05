package com.popIt.pop_it.domain.auth.controller;

import com.popIt.pop_it.domain.auth.dto.AuthReqDTO;
import com.popIt.pop_it.domain.auth.service.AuthService;
import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.exception.code.UserSuccessCode;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        authService.logout(authUser);
        return ApiResponse.onSuccess(UserSuccessCode.USER_LOGOUT, null);
    }

    @PostMapping("/refresh")
    public ApiResponse<UserResDTO.TokenReissueRes> refresh(
            @Valid @RequestBody AuthReqDTO.TokenReissueReq request
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.USER_REISSUE,
                authService.reissue(request.refreshToken())
        );
    }

    @PostMapping("/exchange")
    public ApiResponse<UserResDTO.UserLoginRes> exchange(
            @Valid @RequestBody AuthReqDTO.Exchange request
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.USER_LOGIN,
                authService.exchange(request.code(), request.verifier())
        );
    }
}
