package com.popIt.pop_it.domain.hotspot.dto;

import com.popIt.pop_it.domain.hotspot.enums.HotspotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class HotspotReqDTO {

    public record HotspotCreateReq(
            @NotNull Double positionX,
            @NotNull Double positionY,
            @NotNull Double positionZ,
            @NotBlank @Size(max = 50) String label,
            @NotNull HotspotType type,
            @Size(max = 500) String description, // type=INFO일 때 필수 (서비스 레벨에서 검증)
            Long targetSceneId  // type=LINK일 때 필수 (서비스 레벨에서 검증)
    ) {
    }

    public record HotspotUpdateReq(
            Double positionX,
            Double positionY,
            Double positionZ,
            @Size(min = 1, max = 50) String label,
            @Size(max = 500) String description,  // 기존 type이 INFO일 때만 반영
            Long targetSceneId   // 기존 type이 LINK일 때만 반영
    ) {
    }
}
