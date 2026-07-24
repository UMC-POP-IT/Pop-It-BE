package com.popIt.pop_it.domain.terms.controller;

import com.popIt.pop_it.domain.terms.dto.TermsResDTO;
import com.popIt.pop_it.domain.terms.exception.code.TermsSuccessCode;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Terms", description = "약관 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermsController {

    @Operation(
            summary = "약관 목록 조회",
            description = "프론트에서 노출할 약관 목록을 조회합니다. (현재는 Swagger 매핑용 스텁 — 서비스 구현 예정)"
    )
    @GetMapping
    public ApiResponse<TermsResDTO.ListResult> getTerms() {
        // TODO: 약관 목록 조회 서비스 구현 예정 (현재는 엔드포인트/응답 스키마만 노출)
        return ApiResponse.onSuccess(TermsSuccessCode.TERMS_LIST_FETCHED, new TermsResDTO.ListResult(List.of()));
    }
}
