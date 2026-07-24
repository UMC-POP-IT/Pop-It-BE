package com.popIt.pop_it.domain.user.converter;

import com.popIt.pop_it.domain.user.dto.UserResDTO;

public class UserConverter {
    public static UserResDTO.LoginRes toLogin(String accessToken, String refreshToken) {
        return new UserResDTO.LoginRes(accessToken, refreshToken);
    }

    public static UserResDTO.TokenReissueRes toReissue(String accessToken) {
        return new UserResDTO.TokenReissueRes(accessToken);
    }
}
