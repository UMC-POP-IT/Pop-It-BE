package com.popIt.pop_it.domain.identity_verification.controller;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationReqDTO;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.exception.code.IdentityVerificationSuccessCode;
import com.popIt.pop_it.domain.identity_verification.service.IdentityVerificationService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "본인인증", description = "포트원 기반 본인인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/verifications")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    /**
     * 본인인증 요청 (포트원 API 조회)
     * @param authUser
     * @param dto
     * @return
     */
    @Operation(summary = "본인인증 요청", description = "포트원 API를 통해 인증 정보를 조회합니다. ")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "본인인증 요청 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "identityVerificationId 누락",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "포트원 인증 미완료(VERIFIED 아님), 이미 처리된 인증 건, 이미 가입된 본인인증 정보 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502", description = "포트원 인증 조회 API 연동 오류",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping
    public ApiResponse<IdentityVerificationResDTO.IdentityVerificationVerifyRes> verify(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody @Valid IdentityVerificationReqDTO.IdentityVerificationVerifyReq dto
            ) {
        BaseSuccessCode code = IdentityVerificationSuccessCode.VERIFIED;
        return ApiResponse.onSuccess(code, identityVerificationService.verify(authUser.getUser(), dto));
    }

    /**
     * 본인인증 여부 조회
     * @param authUser
     * @return
     */
    @Operation(summary = "본인인증 여부 조회", description = "본인인증을 했던 적이 있는지 여부를 조회합니다. ")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "본인인증 여부 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping
    public ApiResponse<IdentityVerificationResDTO.IdentityVerificationVerifyRes> isVerified(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        BaseSuccessCode code = IdentityVerificationSuccessCode.IS_VERIFIED;
        return ApiResponse.onSuccess(code, identityVerificationService.isVerified(authUser.getUser()));
    }

}
