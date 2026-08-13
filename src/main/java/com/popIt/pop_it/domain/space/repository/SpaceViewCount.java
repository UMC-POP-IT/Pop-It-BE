package com.popIt.pop_it.domain.space.repository;

// 공간별 조회수 집계용 프로젝션 (가동률 판정)
public interface SpaceViewCount {
    Long getSpaceId();
    Long getViewCount();
}
