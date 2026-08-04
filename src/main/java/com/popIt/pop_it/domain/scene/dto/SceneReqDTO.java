package com.popIt.pop_it.domain.scene.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public class SceneReqDTO {

    public record SceneCreateReq(
            @Schema(description = "씬 이름", example = "1층 로비")
            @NotBlank String name,

            @Schema(description = "사전 제작된 3D 모델(.glb) URL", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/scene/1/uuid.glb")
            @NotBlank @URL String modelUrl,

            @Schema(description = "씬 썸네일 이미지 URL", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/scene/1/uuid.jpg")
            @NotBlank @URL String thumbnail,

            @Schema(description = "초기 카메라 위치 X", example = "0.0")
            @NotNull Double cameraPositionX,

            @Schema(description = "초기 카메라 위치 Y", example = "1.6")
            @NotNull Double cameraPositionY,

            @Schema(description = "초기 카메라 위치 Z", example = "5.0")
            @NotNull Double cameraPositionZ,

            @Schema(description = "카메라가 바라보는 대상 좌표 X", example = "0.0")
            @NotNull Double cameraTargetX,

            @Schema(description = "카메라가 바라보는 대상 좌표 Y", example = "1.0")
            @NotNull Double cameraTargetY,

            @Schema(description = "카메라가 바라보는 대상 좌표 Z", example = "0.0")
            @NotNull Double cameraTargetZ,

            @Schema(description = "카메라 최소 줌 거리", example = "1.0")
            @NotNull Double minDistance,

            @Schema(description = "카메라 최대 줌 거리", example = "10.0")
            @NotNull Double maxDistance,

            @Schema(description = "카메라 최소 수직 회전각(라디안)", example = "0.0")
            @NotNull Double minPolarAngle,

            @Schema(description = "카메라 최대 수직 회전각(라디안)", example = "3.14")
            @NotNull Double maxPolarAngle,

            @Schema(description = "씬 사진 URL 목록. presigned URL로 S3에 이미 업로드 완료한 URL만 전달", nullable = true)
            List<@NotBlank @URL String> imageUrls, // presigned URL로 업로드 완료한 이미지 URL 목록

            @Schema(description = "기본 씬 여부. 생략(null)이면 false로 처리", example = "true", nullable = true)
            Boolean isDefault // null이면 false로 처리
    ) {
    }

    @Schema(description = "씬 수정 요청 (전달한 필드만 반영, 생략하면 기존 값 유지)")
    public record SceneUpdateReq(
            @Schema(description = "씬 이름", example = "1층 로비", nullable = true)
            String name,

            @Schema(description = "3D 모델(.glb) URL", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/scene/1/uuid.glb", nullable = true)
            @URL String modelUrl,

            @Schema(description = "씬 썸네일 이미지 URL", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/scene/1/uuid.jpg", nullable = true)
            @URL String thumbnail,

            @Schema(description = "기본 씬 여부", example = "true", nullable = true)
            Boolean isDefault,
            // 카메라 설정
            @Schema(description = "초기 카메라 위치 X", example = "0.0", nullable = true)
            Double cameraPositionX,

            @Schema(description = "초기 카메라 위치 Y", example = "1.6", nullable = true)
            Double cameraPositionY,

            @Schema(description = "초기 카메라 위치 Z", example = "5.0", nullable = true)
            Double cameraPositionZ,

            @Schema(description = "카메라가 바라보는 대상 좌표 X", example = "0.0", nullable = true)
            Double cameraTargetX,

            @Schema(description = "카메라가 바라보는 대상 좌표 Y", example = "1.0", nullable = true)
            Double cameraTargetY,

            @Schema(description = "카메라가 바라보는 대상 좌표 Z", example = "0.0", nullable = true)
            Double cameraTargetZ,

            @Schema(description = "카메라 최소 줌 거리", example = "1.0", nullable = true)
            Double minDistance,

            @Schema(description = "카메라 최대 줌 거리", example = "10.0", nullable = true)
            Double maxDistance,

            @Schema(description = "카메라 최소 수직 회전각(라디안)", example = "0.0", nullable = true)
            Double minPolarAngle,

            @Schema(description = "카메라 최대 수직 회전각(라디안)", example = "3.14", nullable = true)
            Double maxPolarAngle
    ) {
    }

    public record SceneImageUploadReq(
            @Schema(description = "프론트가 presigned URL로 이미 S3에 업로드 완료한 이미지 URL 목록. 기존 사진 뒤에 이어붙음")
            @NotEmpty List<@NotBlank @URL String> imageUrls
    ) {
    }
}
