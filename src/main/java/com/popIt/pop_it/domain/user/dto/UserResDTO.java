package com.popIt.pop_it.domain.user.dto;

public class UserResDTO {

    public record UserLoginRes(
            String accessToken,
            String refreshToken
    ) {}

    public record TokenReissueRes(
            String accessToken
    ) {}

    public record UserInfoRes(
            Long userId,
            String nickname,
            com.popIt.pop_it.domain.user.entity.enums.UserMode currentMode,
            boolean isHost
    ) {}
}
