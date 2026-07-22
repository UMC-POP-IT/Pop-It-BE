package com.popIt.pop_it.domain.space.controller;

import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.exception.SpaceSuccessCode;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공간")
@RestController
@RequestMapping("/api/v1/spaces")
public class SpaceController {

    // @TODO: AI 맞춤 추천 공간 조회
    @Operation(summary = "AI 맞춤 추천 공간 조회", description = "사용자의 찜/이용 이력을 바탕으로 AI가 추천하는 공간 목록을 조회합니다.<br>"
            + "커서 기반 무한스크롤 방식입니다. (cursor 미전달 시 첫 페이지)")
    @GetMapping("/ai-recommended")
    public ApiResponse<SpaceResDTO.AiRecommendedSpaceList> getAiRecommendedSpaces(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        BaseSuccessCode code = SpaceSuccessCode.AI_RECOMMENDED_SPACE_LIST;

        SpaceResDTO.AiRecommendedSpace space = SpaceResDTO.AiRecommendedSpace.builder()
                .spaceId(15L)
                .buildingName("홍대 팝업 스튜디오")
                .tag("이전에 찜한 공간과 비슷해요")
                .district("마포구")
                .roadAddress("서울특별시 마포구 홍익로 123")
                .exclusiveArea(50.0)
                .basicInfo("POPUP")
                .pricePerDay(700000)
                .pricePerWeek(4200000)
                .pricePerMonth(15000000)
                .thumbnailUrl("https://s3.amazonaws.com/popIt/img5.jpg")
                .parkingAvailable(false)
                .isWishlisted(false)
                .build();

        SpaceResDTO.AiRecommendedSpaceList result = SpaceResDTO.AiRecommendedSpaceList.builder()
                .spaces(List.of(space))
                .hasNext(false)
                .nextCursor(null)
                .build();

        return ApiResponse.onSuccess(code, result);
    }
}
