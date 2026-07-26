package com.popIt.pop_it.domain.scene.dto;

import com.popIt.pop_it.domain.hotspot.dto.HotspotResDTO;
import lombok.Builder;

import java.util.List;

public class SceneResDTO {

    @Builder
    public record SceneSummaryRes(
            Long sceneId,
            String name,
            String thumbnail,
            String modelUrl,
            Boolean isDefault
    ) {
    }

    @Builder
    public record SceneListRes(
            List<SceneSummaryRes> scenes
    ) {
    }

    @Builder
    public record SceneCameraRes(
            List<Double> position,
            List<Double> target,
            Double minDistance,
            Double maxDistance,
            Double minPolarAngle,
            Double maxPolarAngle
    ) {
    }

    @Builder
    public record SceneDetailRes(
            Long sceneId,
            String name,
            String modelUrl,
            Boolean isDefault,
            SceneCameraRes camera,
            List<HotspotResDTO.HotspotSummaryRes> hotspots
    ) {
    }

    @Builder
    public record SceneIdRes(
            Long sceneId
    ) {
    }

    @Builder
    public record SceneImageUploadRes(
            Long sceneId,
            List<String> images
    ) {
    }
}
