package com.popIt.pop_it.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProvider {
    KAKAO("카카오"),
    GOOGLE("구글");

    private final String description;
}
