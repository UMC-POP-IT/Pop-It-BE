package com.popIt.pop_it.domain.user.dto;

public class UserResDTO {

    public record Login(
            String accessToken
    ) {}
}
