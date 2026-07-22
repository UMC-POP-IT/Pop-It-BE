package com.popIt.pop_it.domain.space.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class SpaceResDTO {

    @Schema(description = "공간 등록 응답")
    @Builder
    public record CreateResult(
            @Schema(description = "등록된 공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "합정 메세나폴리스")
            String buildingName
    ) {}

    @Builder
    public record AiRecommendedSpace(
            Long spaceId,
            String buildingName,
            String tag,
            String district,
            String roadAddress,
            Double exclusiveArea,
            String basicInfo,
            Integer pricePerDay,
            Integer pricePerWeek,
            Integer pricePerMonth,
            String thumbnailUrl,
            Boolean parkingAvailable,
            Boolean isWishlisted
    ) {}

    @Builder
    public record AiRecommendedSpaceList(
            List<AiRecommendedSpace> spaces,
            Boolean hasNext,
            String nextCursor
    ) {}
}
