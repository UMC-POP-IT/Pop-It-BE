package com.popIt.pop_it.domain.upload.controller;

import com.popIt.pop_it.domain.upload.dto.UploadReqDTO;
import com.popIt.pop_it.domain.upload.dto.UploadResDTO;
import com.popIt.pop_it.domain.upload.exception.code.UploadSuccessCode;
import com.popIt.pop_it.domain.upload.service.UploadService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "파일 업로드 공통 API")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/presigned-url")
    @Operation(summary = "presigned URL 발급", description = "S3 직접 업로드를 위한 presigned URL을 발급합니다.")
    public ApiResponse<UploadResDTO.PresignedUrlList> issuePresignedUrls(
            @Valid @RequestBody UploadReqDTO.PresignedUrl request
    ) {
        UploadResDTO.PresignedUrlList result =  uploadService.issuePresignedUrls(request);
        return ApiResponse.onSuccess(UploadSuccessCode.PRESIGNED_URL_ISSUED, result);
    }
}
