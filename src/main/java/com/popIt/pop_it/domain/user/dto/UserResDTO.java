package com.popIt.pop_it.domain.user.dto;

import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserResDTO {

    public record UserLoginRes(
            @Schema(description = "액세스 토큰 (Authorization 헤더에 Bearer로 사용)", example = "eyJhbGciOiJIUzI1NiJ9...")
            String accessToken,

            @Schema(description = "리프레시 토큰 (액세스 토큰 재발급 시 사용)", example = "eyJhbGciOiJIUzI1NiJ9...")
            String refreshToken
    ) {}

    public record TokenReissueRes(
            @Schema(description = "재발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
            String accessToken
    ) {}

    public record UserInfoRes(
            @Schema(description = "유저 ID", example = "1")
            Long userId,

            @Schema(description = "닉네임", example = "홍길동")
            String nickname,

            @Schema(description = "현재 활동 모드", example = "GUEST")
            UserMode currentMode,

            @Schema(description = "호스트 프로필 등록 여부. currentMode와 별개로, 프론트의 모드 전환/호스트 등록 온보딩 분기에 사용 (예: currentMode=GUEST이면서 true 가능)", example = "false")
            boolean hasHostProfile
    ) {}
}
