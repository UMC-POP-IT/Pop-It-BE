package com.popIt.pop_it.domain.user_agreement.controller;

import com.popIt.pop_it.domain.user_agreement.dto.AgreementReqDTO;
import com.popIt.pop_it.domain.user_agreement.dto.AgreementResDTO;
import com.popIt.pop_it.domain.user_agreement.exception.code.AgreementSuccessCode;
import com.popIt.pop_it.domain.user_agreement.service.AgreementService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agreement", description = "약관 동의 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agreements")
public class AgreementController {

    private final AgreementService agreementService;

    @Operation(
            summary = "약관 동의 저장",
            description = """
                    로그인한 사용자의 약관 동의 정보를 저장합니다.
                    - 인증 필요: 우측 상단 Authorize에 Access Token을 입력하세요.
                    - 약관은 1개 이상 여러 개를 한 번에 동의할 수 있습니다.
                    - 이미 동의한 약관을 다시 요청하면 동의 여부/시각이 갱신됩니다.
                    - 필수 약관은 미동의(false)로 저장할 수 없습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "약관 동의 저장 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 약관 미동의/형식 오류", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않음", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 약관 포함", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping
    public ApiResponse<AgreementResDTO.SaveResult> saveAgreements(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody AgreementReqDTO.Save request
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        AgreementResDTO.SaveResult result = agreementService.save(userId, request);

        return ApiResponse.onSuccess(AgreementSuccessCode.AGREEMENT_SAVED, result);
    }
}
