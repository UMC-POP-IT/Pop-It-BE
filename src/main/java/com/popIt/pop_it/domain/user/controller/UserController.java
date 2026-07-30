package com.popIt.pop_it.domain.user.controller;

import com.popIt.pop_it.domain.user.dto.UserReqDTO;
import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.exception.code.UserSuccessCode;
import com.popIt.pop_it.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "활동 모드 전환",
            description = """
                    로그인한 사용자의 활동 모드(HOST/GUEST)를 전환합니다.
                    - 인증 필요: 우측 상단 Authorize에 로그인으로 발급받은 Access Token을 입력하세요.
                    - HOST로 전환하려면 호스트 프로필이 등록되어 있어야 합니다(없으면 403).
                    - GUEST로 전환은 항상 허용되며, 이미 같은 모드여도 200을 반환합니다(멱등).
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모드 전환 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수값 누락/형식 오류", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않음", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "호스트 프로필 없음(HOST 전환 불가)", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PatchMapping("/me/mode")
    public ResponseEntity<ApiResponse<UserResDTO.UserInfoRes>> switchMode(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UserReqDTO.ModeSwitchReq request
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        UserResDTO.UserInfoRes result = userService.switchMode(userId, request.mode());

        return ResponseEntity.ok(ApiResponse.onSuccess(UserSuccessCode.USER_MODE_SWITCH, result));
    }

    @Operation(
            summary = "내 정보 조회",
            description = """
                    로그인한 사용자의 기본 정보를 조회합니다. (새로고침 후 세션 복구/토큰 유효성 프로브 용도)
                    - 인증 필요: 우측 상단 Authorize에 로그인으로 발급받은 Access Token을 입력하세요.
                    - hasHostProfile: 호스트 프로필 등록 여부입니다. 현재 활동 모드(currentMode)와 별개로,
                      프론트의 "모드 전환" 노출 / "호스트 등록" 온보딩 분기에 사용합니다.
                      (예: currentMode=GUEST 이면서 hasHostProfile=true 가능)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않음", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResDTO.UserInfoRes>> getMyInfo(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        UserResDTO.UserInfoRes result = userService.getMyInfo(userId);

        return ResponseEntity.ok(ApiResponse.onSuccess(UserSuccessCode.USER_GET_ME, result));
    }
}
