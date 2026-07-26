package com.popIt.pop_it.domain.scene.dto;

import lombok.Builder;

import java.util.List;

public class SceneResDTO {

    @Builder
    public record Summary(
            Long sceneId,
            String name,
            String thumbnail,
            String modelUrl,
            Boolean isDefault
    ) {
    }

    @Builder
    public record SceneList(
            List<Summary> scenes
    ) {
    }

    @Builder
    public record Camera(
            List<Double> position,
            List<Double> target,
            Double minDistance,
            Double maxDistance,
            Double minPolarAngle,
            Double maxPolarAngle
    ) {
    }

    @Builder
    public record Detail(
            Long sceneId,
            String name,
            String modelUrl,
            Boolean isDefault,
            Camera camera,
            List<Object> hotspots // TODO: Hotspot 도메인 구현 후 List<HotspotResDTO.Summary>로 교체
    ) {
    }

    @Builder
    public record SceneId(
            Long sceneId
    ) {
    }

    @Builder
    public record ImageUploadResult(
            Long sceneId,
            List<String> images
    ) {
    }
}
