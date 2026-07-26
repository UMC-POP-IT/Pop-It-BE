package com.popIt.pop_it.domain.scene.dto;

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
            List<Object> hotspots // TODO: Hotspot 도메인 구현 후 List<HotspotResDTO.Summary>로 교체
    ) {
    }

    @Builder
    public record SceneIdRes(
            Long sceneId
    ) {
    }

    @Builder
    public record ImageUploadResultRes(
            Long sceneId,
            List<String> images
    ) {
    }
}
