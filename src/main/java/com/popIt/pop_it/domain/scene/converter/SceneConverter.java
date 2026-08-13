package com.popIt.pop_it.domain.scene.converter;

import com.popIt.pop_it.domain.hotspot.converter.HotspotConverter;
import com.popIt.pop_it.domain.hotspot.entity.Hotspot;
import com.popIt.pop_it.domain.scene.dto.SceneResDTO;
import com.popIt.pop_it.domain.scene.entity.Scene;

import java.util.List;

public class SceneConverter {
    public static SceneResDTO.SceneSummaryRes toSummary(Scene scene) {
        return SceneResDTO.SceneSummaryRes.builder()
                .sceneId(scene.getId())
                .name(scene.getName())
                .thumbnail(scene.getThumbnail())
                .modelUrl(scene.getModelUrl())
                .isDefault(scene.getIsDefault())
                .build();
    }

    public static SceneResDTO.SceneListRes toSceneList(List<Scene> scenes) {
        List<SceneResDTO.SceneSummaryRes> summaries = scenes.stream()
                .map(SceneConverter::toSummary)
                .toList();

        return SceneResDTO.SceneListRes.builder()
                .scenes(summaries)
                .build();
    }

    public static SceneResDTO.SceneDetailRes toDetail(Scene scene, List<Hotspot> hotspots) {
        SceneResDTO.SceneCameraRes camera = SceneResDTO.SceneCameraRes.builder()
                .position(List.of(scene.getCameraPositionX(), scene.getCameraPositionY(), scene.getCameraPositionZ()))
                .target(List.of(scene.getCameraTargetX(), scene.getCameraTargetY(), scene.getCameraTargetZ()))
                .minDistance(scene.getMinDistance())
                .maxDistance(scene.getMaxDistance())
                .minPolarAngle(scene.getMinPolarAngle())
                .maxPolarAngle(scene.getMaxPolarAngle())
                .build();

        return SceneResDTO.SceneDetailRes.builder()
                .sceneId(scene.getId())
                .name(scene.getName())
                .modelUrl(scene.getModelUrl())
                .isDefault(scene.getIsDefault())
                .camera(camera)
                .hotspots(HotspotConverter.toSummaryList(hotspots))
                .build();
    }

    public static SceneResDTO.SceneIdRes toSceneId(Scene scene) {
        return SceneResDTO.SceneIdRes.builder()
                .sceneId(scene.getId())
                .build();
    }

    public static SceneResDTO.ImageUploadResultRes toImageUploadResult(Long sceneId, List<String> imageUrls) {
        return SceneResDTO.ImageUploadResultRes.builder()
                .sceneId(sceneId)
                .images(imageUrls)
                .build();
    }
}
