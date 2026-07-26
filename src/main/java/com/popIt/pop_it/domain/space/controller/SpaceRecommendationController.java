package com.popIt.pop_it.domain.space.controller;

import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.exception.SpaceSuccessCode;
import com.popIt.pop_it.domain.space.service.SpaceRecommendationService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공간")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces")
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
    @GetMapping("/ai-recommended")
    public ApiResponse<SpaceResDTO.AiRecommendedSpaceListRes> getAiRecommendedSpaces(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        BaseSuccessCode code = SpaceSuccessCode.AI_RECOMMENDED_SPACE_LIST;
        Long userId = authUser.getUser().getUserId();
        SpaceResDTO.AiRecommendedSpaceListRes result =
                spaceRecommendationService.getRecommendedSpaces(userId, cursor, size);

        return ApiResponse.onSuccess(code, result);
    }
}
