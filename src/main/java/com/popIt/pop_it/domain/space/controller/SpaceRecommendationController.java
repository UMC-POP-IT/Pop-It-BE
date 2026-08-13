package com.popIt.pop_it.domain.space.controller;

import com.popIt.pop_it.domain.space.dto.AiRecommendationResDTO;
import com.popIt.pop_it.domain.space.exception.SpaceSuccessCode;
import com.popIt.pop_it.domain.space.service.SpaceRecommendationService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공간", description = "공간 등록/조회/수정/삭제 및 탐색/추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces")
@Validated
public class SpaceRecommendationController {

    private final SpaceRecommendationService spaceRecommendationService;

    @Operation(
            summary = "AI 맞춤 추천 공간 조회",
            description = """
                    사용자의 찜/조회 이력을 바탕으로 AI가 추천하는 공간 목록을 조회합니다.
                    - 유사도가 일정 기준 미만인 공간은 추천 목록에서 제외됩니다. 기준을 넘는 공간이 없으면 빈 목록을 반환합니다.
                    - 찜/조회 이력은 기간 제한 없이 전체 반영되며, 오래된 이력일수록 가중치가 감쇠합니다.
                    - 찜/조회 이력이 전혀 없는 신규 유저는 빈 목록을 반환합니다. 이 경우 hasActivityHistory를 false로 반환합니다.
                    - 커서 기반 무한스크롤 방식입니다. (cursor 미전달 시 첫 페이지)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "AI 맞춤 추천 공간 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "잘못된 커서 값, size 파라미터 범위(1~10) 초과 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping("/ai-recommended")
    public ApiResponse<AiRecommendationResDTO.AiRecommendedSpaceListRes> getAiRecommendedSpaces(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int size
    ) {
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        BaseSuccessCode code = SpaceSuccessCode.AI_RECOMMENDED_SPACE_LIST;
        Long userId = authUser.getUser().getUserId();
        AiRecommendationResDTO.AiRecommendedSpaceListRes result =
                spaceRecommendationService.getRecommendedSpaces(userId, cursor, size);

        return ApiResponse.onSuccess(code, result);
    }
}
