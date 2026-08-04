package com.popIt.pop_it.domain.scene.dto;

import com.popIt.pop_it.domain.hotspot.dto.HotspotResDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class SceneResDTO {

    @Builder
    public record SceneSummaryRes(
            @Schema(description = "씬 ID", example = "1")
            Long sceneId,

            @Schema(description = "씬 이름", example = "1층 로비")
            String name,

            @Schema(description = "씬 썸네일 이미지 URL", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/scene/1/uuid.jpg")
            String thumbnail,

            @Schema(description = "3D 모델(.glb) URL", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/scene/1/uuid.glb")
            String modelUrl,

            @Schema(description = "기본 씬 여부", example = "true")
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
            @Schema(description = "초기 카메라 위치 [x, y, z]", example = "[0.0, 1.6, 5.0]")
            List<Double> position,

            @Schema(description = "카메라가 바라보는 대상 좌표 [x, y, z]", example = "[0.0, 1.0, 0.0]")
            List<Double> target,

            @Schema(description = "카메라 최소 줌 거리", example = "1.0")
            Double minDistance,

            @Schema(description = "카메라 최대 줌 거리", example = "10.0")
            Double maxDistance,

            @Schema(description = "카메라 최소 수직 회전각(라디안)", example = "0.0")
            Double minPolarAngle,

            @Schema(description = "카메라 최대 수직 회전각(라디안)", example = "3.14")
            Double maxPolarAngle
    ) {
    }

    @Builder
    public record SceneDetailRes(
            @Schema(description = "씬 ID", example = "1")
            Long sceneId,

            @Schema(description = "씬 이름", example = "1층 로비")
            String name,

            @Schema(description = "3D 모델(.glb) URL", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/scene/1/uuid.glb")
            String modelUrl,

            @Schema(description = "기본 씬 여부", example = "true")
            Boolean isDefault,

            @Schema(description = "초기 카메라 설정")
            SceneCameraRes camera,

            @Schema(description = "씬에 등록된 핫스팟 목록")
            List<HotspotResDTO.HotspotSummaryRes> hotspots
    ) {
    }

    @Builder
    public record SceneIdRes(
            @Schema(description = "생성/수정된 씬 ID", example = "1")
            Long sceneId
    ) {
    }

    @Builder
    public record ImageUploadResultRes(
            @Schema(description = "씬 ID", example = "1")
            Long sceneId,

            @Schema(description = "등록 완료 후 씬의 전체 사진 URL 목록 (기존 사진 포함)")
            List<String> images
    ) {
    }
}
