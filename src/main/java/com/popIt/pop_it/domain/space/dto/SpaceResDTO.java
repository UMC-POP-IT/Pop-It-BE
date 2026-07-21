package com.popIt.pop_it.domain.space.dto;

import lombok.Builder;

import java.util.List;

public class SpaceResDTO {

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
