package com.popIt.pop_it.domain.upload.controller;

import com.popIt.pop_it.domain.upload.dto.UploadReqDTO;
import com.popIt.pop_it.domain.upload.dto.UploadResDTO;
import com.popIt.pop_it.domain.upload.exception.code.UploadSuccessCode;
import com.popIt.pop_it.domain.upload.service.UploadService;
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

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "업로드", description = "파일 업로드 공통 API")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/presigned-url")
    @Operation(
            summary = "presigned URL 발급",
            description = """
                    S3 직접 업로드를 위한 presigned URL을 발급합니다.
                    - 인증 필요: 우측 상단 Authorize에 Access Token을 입력하세요.
                    - 파일은 유저별 경로({uploadType}/{userId}/{uuid})에 저장됩니다.
                    - 발급된 presignedUrl로 프론트에서 직접 S3에 PUT 업로드 후, fileUrl을 호스트 등록 API에 전달하세요.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "presigned URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 입력값 누락/형식 오류(업로드 타입, 파일 목록, contentType), 최대 개수(10개) 초과 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ApiResponse<UploadResDTO.PresignedUrlListRes> issuePresignedUrls(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UploadReqDTO.PresignedUrlReq request
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        UploadResDTO.PresignedUrlListRes result = uploadService.issuePresignedUrls(userId, request);
        return ApiResponse.onSuccess(UploadSuccessCode.PRESIGNED_URL_ISSUED, result);
    }
}
