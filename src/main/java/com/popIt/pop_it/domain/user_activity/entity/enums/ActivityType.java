package com.popIt.pop_it.domain.user_activity.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityType {
    VIEW("조회");

    private final String description;
}
