package com.popIt.pop_it.domain.scene.converter;

import com.popIt.pop_it.domain.scene.dto.SceneResDTO;
import com.popIt.pop_it.domain.scene.entity.Scene;

import java.util.List;

public class SceneConverter {
    public static SceneResDTO.Summary toSummary(Scene scene) {
        return SceneResDTO.Summary.builder()
                .sceneId(scene.getId())
                .name(scene.getName())
                .thumbnail(scene.getThumbnail())
                .modelUrl(scene.getModelUrl())
                .isDefault(scene.getIsDefault())
                .build();
    }

    public static SceneResDTO.SceneList toSceneList(List<Scene> scenes) {
        List<SceneResDTO.Summary> summaries = scenes.stream()
                .map(SceneConverter::toSummary)
                .toList();

        return SceneResDTO.SceneList.builder()
                .scenes(summaries)
                .build();
    }

    public static SceneResDTO.Detail toDetail(Scene scene) {
        SceneResDTO.Camera camera = SceneResDTO.Camera.builder()
                .position(List.of(scene.getCameraPositionX(), scene.getCameraPositionY(), scene.getCameraPositionZ()))
                .target(List.of(scene.getCameraTargetX(), scene.getCameraTargetY(), scene.getCameraTargetZ()))
                .minDistance(scene.getMinDistance())
                .maxDistance(scene.getMaxDistance())
                .minPolarAngle(scene.getMinPolarAngle())
                .maxPolarAngle(scene.getMaxPolarAngle())
                .build();

        return SceneResDTO.Detail.builder()
                .sceneId(scene.getId())
                .name(scene.getName())
                .modelUrl(scene.getModelUrl())
                .isDefault(scene.getIsDefault())
                .camera(camera)
                .hotspots(List.of()) // TODO: Hotspot 도메인 구현 후 실제 목록으로 교체
                .build();
    }

    public static SceneResDTO.SceneId toSceneId(Scene scene) {
        return SceneResDTO.SceneId.builder()
                .sceneId(scene.getId())
                .build();
    }

    public static SceneResDTO.ImageUploadResult toImageUploadResult(Long sceneId, List<String> imageUrls) {
        return SceneResDTO.ImageUploadResult.builder()
                .sceneId(sceneId)
                .images(imageUrls)
                .build();
    }
}
