package com.popIt.pop_it.domain.user.converter;

import com.popIt.pop_it.domain.user.dto.UserResDTO;
import com.popIt.pop_it.domain.user.entity.User;

public class UserConverter {
    public static UserResDTO.UserLoginRes toLogin(String accessToken, String refreshToken) {
        return new UserResDTO.UserLoginRes(accessToken, refreshToken);
    }

    public static UserResDTO.TokenReissueRes toReissue(String accessToken) {
        return new UserResDTO.TokenReissueRes(accessToken);
    }

    public static UserResDTO.UserInfoRes toUserInfo(User user, boolean hasHostProfile) {
        return new UserResDTO.UserInfoRes(
                user.getUserId(),
                user.getNickname(),
                user.getCurrentMode(),
                hasHostProfile
        );
    }
}
