package com.popIt.pop_it.domain.hotspot.dto;

import com.popIt.pop_it.domain.hotspot.enums.HotspotType;
import lombok.Builder;

import java.util.List;

public class HotspotResDTO {

    @Builder
    public record HotspotSummaryRes(
            Long hotspotId,
            List<Double> position,
            HotspotType type,
            String label,
            String description,
            Long targetSceneId
    ) {
    }

    @Builder
    public record HotspotIdRes(
            Long hotspotId
    ) {
    }
}
