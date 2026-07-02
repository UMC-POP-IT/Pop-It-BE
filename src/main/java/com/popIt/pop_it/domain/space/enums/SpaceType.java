package com.popIt.pop_it.domain.space.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpaceType {

    OPEN_HALL("오픈형 홀"),
    PARTITION_WALL("가벽 분리형"),
    ROOM_SEPARATED("룸 분리형")
    ;

    private final String description;
}
