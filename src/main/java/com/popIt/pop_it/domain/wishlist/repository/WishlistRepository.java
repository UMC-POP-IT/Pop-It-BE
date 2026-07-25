package com.popIt.pop_it.domain.wishlist.repository;

import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 공간 상세, 탐색 응답의 wishCount (로그인 여부와 상관 X)
    long countBySpaceId(Long spaceId);

    // 공간 상세, 탐색 응답의 isWishlisted (로그인 사용자 기준)
    boolean existsByUserIdAndSpaceId(Long userId, Long spaceId);

    // 찜 토글 해제 시 사용 (삭제된 행 수 반환)
    long deleteByUserIdAndSpaceId(Long userId, Long spaceId);
}
