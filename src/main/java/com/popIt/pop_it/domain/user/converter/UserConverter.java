package com.popIt.pop_it.domain.user.converter;

import com.popIt.pop_it.domain.user.dto.UserResDTO;

public class UserConverter {
    public static UserResDTO.Login toLogin(String accessToken) {
        return new UserResDTO.Login(accessToken);
    }
}
