package com.popIt.pop_it.domain.hotspot.dto;

import com.popIt.pop_it.domain.hotspot.enums.HotspotType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class HotspotReqDTO {

    public record HotspotCreateReq(
            @Schema(description = "핫스팟의 씬 내 3D 좌표 X", example = "1.5")
            @NotNull Double positionX,

            @Schema(description = "핫스팟의 씬 내 3D 좌표 Y", example = "0.0")
            @NotNull Double positionY,

            @Schema(description = "핫스팟의 씬 내 3D 좌표 Z", example = "-2.3")
            @NotNull Double positionZ,

            @Schema(description = "핫스팟 라벨(마커에 표시되는 짧은 이름)", example = "출입구")
            @NotBlank @Size(max = 50) String label,

            @Schema(description = "핫스팟 유형. INFO: 클릭 시 설명 텍스트 표시, LINK: 클릭 시 다른 씬(방)으로 이동", example = "INFO")
            @NotNull HotspotType type,

            @Schema(description = "설명 텍스트. type=INFO일 때 필수", example = "이 공간의 정문입니다.", nullable = true)
            @Size(max = 500) String description, // type=INFO일 때 필수 (서비스 레벨에서 검증)

            @Schema(description = "이동 대상 씬 ID. type=LINK일 때 필수", example = "5", nullable = true)
            Long targetSceneId  // type=LINK일 때 필수 (서비스 레벨에서 검증)
    ) {
    }

    public record HotspotUpdateReq(
            @Schema(description = "핫스팟의 씬 내 3D 좌표 X (생략 시 기존 값 유지)", example = "1.5", nullable = true)
            Double positionX,

            @Schema(description = "핫스팟의 씬 내 3D 좌표 Y (생략 시 기존 값 유지)", example = "0.0", nullable = true)
            Double positionY,

            @Schema(description = "핫스팟의 씬 내 3D 좌표 Z (생략 시 기존 값 유지)", example = "-2.3", nullable = true)
            Double positionZ,

            @Schema(description = "핫스팟 라벨 (생략 시 기존 값 유지)", example = "출입구", nullable = true)
            @Pattern(regexp = ".*\\S.*", message = "공백만으로는 입력할 수 없습니다.") @Size(max = 50) String label,

            @Schema(description = "설명 텍스트. 기존 type이 INFO일 때만 반영됨 (생략 시 기존 값 유지)", example = "이 공간의 정문입니다.", nullable = true)
            @Pattern(regexp = ".*\\S.*", message = "공백만으로는 입력할 수 없습니다.") @Size(max = 500) String description,  // 기존 type이 INFO일 때만 반영

            @Schema(description = "이동 대상 씬 ID. 기존 type이 LINK일 때만 반영됨 (생략 시 기존 값 유지)", example = "5", nullable = true)
            Long targetSceneId   // 기존 type이 LINK일 때만 반영
    ) {
    }
}
