package com.popIt.pop_it.domain.user.dto;

public class UserResDTO {

    public record LoginRes(
            String accessToken,
            String refreshToken
    ) {}

    public record TokenReissueRes(
            String accessToken
    ) {}
}
