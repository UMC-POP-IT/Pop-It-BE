package com.popIt.pop_it.domain.identity_verification.controller;

import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationReqDTO;
import com.popIt.pop_it.domain.identity_verification.dto.IdentityVerificationResDTO;
import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.identity_verification.exception.code.IdentityVerificationSuccessCode;
import com.popIt.pop_it.domain.identity_verification.service.IdentityVerificationService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/verifications")
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    @PostMapping
    public ApiResponse<IdentityVerificationResDTO.Verify> verify(
            @RequestBody @Valid IdentityVerificationReqDTO.Verify dto
            ) {
        BaseSuccessCode code = IdentityVerificationSuccessCode.VERIFIED;
        return ApiResponse.onSuccess(code, identityVerificationService.verify(dto));
    }
}
