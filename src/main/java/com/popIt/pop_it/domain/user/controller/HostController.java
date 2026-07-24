package com.popIt.pop_it.domain.user.controller;

import com.popIt.pop_it.domain.user.dto.HostProfileResponse;
import com.popIt.pop_it.domain.user.dto.HostRegisterRequest;
import com.popIt.pop_it.domain.user.dto.HostRegisterResponse;
import com.popIt.pop_it.domain.user.exception.code.HostSuccessCode;
import com.popIt.pop_it.domain.user.service.HostService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Host", description = "호스트 등록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hosts")
public class HostController {

    private final HostService hostService;

    // 호스트 등록 (인증 필요: Access Token)
    @Operation(
            summary = "호스트 등록",
            description = """
                    로그인된 사용자를 호스트로 등록합니다.
                    - 인증 필요: 우측 상단 Authorize에 로그인으로 발급받은 Access Token을 입력하세요.
                    - 한 사용자당 호스트 프로필은 1개만 허용됩니다(중복 시 409).
                    - 등록 성공 시 사용자의 활동 모드가 HOST로 전환됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "호스트 등록 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수값 누락/형식 오류", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않음", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 호스트 프로필", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping
    public ResponseEntity<ApiResponse<HostRegisterResponse>> registerHost(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HostRegisterRequest request
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        HostRegisterResponse result = hostService.register(userId, request);

        return ResponseEntity.status(HostSuccessCode.HOST_REGISTER.getStatus())
                .body(ApiResponse.onSuccess(HostSuccessCode.HOST_REGISTER, result));
    }

    @Operation(
            summary = "내 호스트 프로필 조회",
            description = "로그인한 사용자의 호스트 프로필을 조회합니다. 인증 필요: Access Token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않음", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "호스트 프로필 없음", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<HostProfileResponse>> getMyProfile(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        HostProfileResponse result = hostService.getMyProfile(userId);

        return ResponseEntity.ok(ApiResponse.onSuccess(HostSuccessCode.HOST_GET, result));
    }
}
