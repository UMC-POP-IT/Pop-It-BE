package com.popIt.pop_it.domain.wishlist.repository;

// 공간별 찜 수 집계용 프로젝션 (목록 조회 시 배치 집계)
public interface WishCountBySpace {
    Long getSpaceId();
    Long getCount();
}
