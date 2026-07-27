package com.popIt.pop_it.domain.user_activity.repository;

// 공간별 조회수 집계용 프로젝션
public interface SpaceViewCount {
    Long getSpaceId();
    Long getViewCount();
}
