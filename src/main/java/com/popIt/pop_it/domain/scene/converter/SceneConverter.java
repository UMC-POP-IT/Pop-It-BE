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
                .build();

        return SceneResDTO.Detail.builder()
                .sceneId(scene.getId())
                .name(scene.getName())
                .modelUrl(scene.getModelUrl())
                .isDefault(scene.getIsDefault())
                .camera(camera)
                // TODO:hotspots는 후속 이슈 완료 전까지 Controller/Service에서 빈 리스트로 채움
                .build();
    }

    public static SceneResDTO.SceneId toSceneId(Scene scene) {
        return SceneResDTO.SceneId.builder()
                .sceneId(scene.getId())
                .build();
    }
}
