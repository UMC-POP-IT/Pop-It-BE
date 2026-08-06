package com.popIt.pop_it.domain.hotspot.dto;

import com.popIt.pop_it.domain.hotspot.enums.HotspotType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class HotspotResDTO {

    @Builder
    public record HotspotSummaryRes(
            @Schema(description = "핫스팟 ID", example = "3")
            Long hotspotId,

            @Schema(description = "씬 내 3D 좌표 [x, y, z]", example = "[1.5, 0.0, -2.3]")
            List<Double> position,

            @Schema(description = "핫스팟 유형. INFO: 설명 텍스트 표시, LINK: 다른 씬으로 이동", example = "INFO")
            HotspotType type,

            @Schema(description = "핫스팟 라벨", example = "출입구")
            String label,

            @Schema(description = "설명 텍스트. type=LINK면 null", example = "이 공간의 정문입니다.", nullable = true)
            String description,

            @Schema(description = "이동 대상 씬 ID. type=INFO면 null", example = "5", nullable = true)
            Long targetSceneId
    ) {
    }

    @Builder
    public record HotspotIdRes(
            @Schema(description = "생성/수정된 핫스팟 ID", example = "3")
            Long hotspotId
    ) {
    }
}
