package com.popIt.pop_it.domain.auth.controller;

import com.popIt.pop_it.domain.auth.dto.AuthReqDTO;
import com.popIt.pop_it.domain.auth.service.AuthService;
import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.exception.code.UserSuccessCode;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인/로그아웃/토큰 재발급 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그아웃", description = "저장된 리프레시 토큰을 무효화합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        authService.logout(authUser);
        return ApiResponse.onSuccess(UserSuccessCode.USER_LOGOUT, null);
    }

    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 재발급", description = "유효한 리프레시 토큰으로 새 액세스 토큰을 발급받습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "액세스 토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "refreshToken 누락",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 리프레시 토큰, 일치하지 않는 리프레시 토큰 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ApiResponse<UserResDTO.TokenReissueRes> refresh(
            @Valid @RequestBody AuthReqDTO.TokenReissueReq request
    ) {
        return ApiResponse.onSuccess(
                UserSuccessCode.USER_REISSUE,
                authService.reissue(request.refreshToken())
        );
    }

    @Operation(
            summary = "소셜 로그인 코드 교환",
            description = "소셜 로그인(OAuth) 성공 후 리다이렉트로 전달받은 1회용 code를 실제 토큰으로 교환합니다.<br>"
                    + "code는 발급 후 30초 이내, 1회만 사용 가능합니다. verifier는 로그인 시작 시 프론트가 생성해둔 값을 그대로 전달해야 합니다(PKCE 유사 검증)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "소셜 로그인 코드 교환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "code/verifier 누락, 유효하지 않거나 만료되었거나 이미 사용된 code 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
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
