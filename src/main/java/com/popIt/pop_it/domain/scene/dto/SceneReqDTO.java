package com.popIt.pop_it.domain.scene.dto;

import jakarta.validation.constraints.NotBlank;
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
            List<String> imageUrls, // presigned URL로 업로드 완료한 이미지 URL 목록
            Boolean isDefault // null이면 false로 처리
    ) {
    }

    public record Update(
            String name,
            String modelUrl,
            String thumbnail,
            Boolean isDefault
    ) {
    }
}
