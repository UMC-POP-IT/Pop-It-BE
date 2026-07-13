package com.popIt.pop_it.domain.identity_verification.controller;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationReqDTO;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.exception.code.IdentityVerificationSuccessCode;
import com.popIt.pop_it.domain.identity_verification.service.IdentityVerificationService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "본인인증", description = "")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/verifications")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    /**
     * 본인인증 요청 (포트원 API 조회)
     * @param authUser
     * @param dto
     * @return
     */
    @Operation(summary = "본인인증 요청", description = "포트원 API를 통해 인증 정보를 조회합니다. ")
    @PostMapping
    public ApiResponse<IdentityVerificationResDTO.Verify> verify(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody @Valid IdentityVerificationReqDTO.Verify dto
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
    @GetMapping
    @PostMapping
    public ApiResponse<IdentityVerificationResDTO.Verify> isVerified(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        BaseSuccessCode code = IdentityVerificationSuccessCode.ISVERIFIED;
        return ApiResponse.onSuccess(code, identityVerificationService.isVerified(authUser.getUser()));
    }

}
