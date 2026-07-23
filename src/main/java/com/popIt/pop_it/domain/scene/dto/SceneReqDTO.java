package com.popIt.pop_it.domain.scene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SceneReqDTO {

    public record Create(
            @NotBlank String name,
            @NotBlank String modelUrl,
            @NotBlank String thumbnail,
            @NotNull Double cameraPositionX,
            @NotNull Double cameraPositionY,
            @NotNull Double cameraPositionZ,
            @NotNull Double cameraTargetX,
            @NotNull Double cameraTargetY,
            @NotNull Double cameraTargetZ,
            @NotNull Double minDistance,
            @NotNull Double maxDistance,
            @NotNull Double minPolarAngle,
            @NotNull Double maxPolarAngle,
            List<String> imageUrls, // presigned URL로 업로드 완료한 이미지 URL 목록
            Boolean isDefault // null이면 false로 처리
    ) {
    }

    public record Update(
            String name,
            String modelUrl,
            String thumbnail,
            Boolean isDefault,
            // 카메라 설정 - modelUrl을 바꿀 때는 함께 보내야 함 (모델마다 크기/형태가 달라 카메라 값이 어긋날 수 있음)
            Double cameraPositionX,
            Double cameraPositionY,
            Double cameraPositionZ,
            Double cameraTargetX,
            Double cameraTargetY,
            Double cameraTargetZ,
            Double minDistance,
            Double maxDistance,
            Double minPolarAngle,
            Double maxPolarAngle
    ) {
    }

    public record ImageUpload(
            @NotEmpty List<@NotBlank String> imageUrls // 프론트가 presigned URL로 이미 업로드 완료한 이미지 URL 목록
    ) {
    }
}
