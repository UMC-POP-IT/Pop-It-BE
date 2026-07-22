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
            Double maxDistance
    ) {
    }

    @Builder
    public record Detail(
            Long sceneId,
            String name,
            String modelUrl,
            Boolean isDefault,
            Camera camera
            // TODO: hotspots는 후속 이슈(Hotspot 도메인) 완료 전까지 빈 리스트로
    ) {
    }

    @Builder
    public record SceneId(
            Long sceneId
    ) {
    }
}
