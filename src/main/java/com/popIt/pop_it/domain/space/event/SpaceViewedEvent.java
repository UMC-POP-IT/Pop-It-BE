package com.popIt.pop_it.domain.space.event;

import com.popIt.pop_it.domain.user.entity.enums.UserMode;

public record SpaceViewedEvent(
    Long spaceId,
    Long userId,
    UserMode viewerMode
) {}
