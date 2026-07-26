package com.popIt.pop_it.domain.wishlist.repository;

import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 공간 상세, 탐색 응답의 wishCount (로그인 여부와 상관 X)
    long countBySpaceId(Long spaceId);

    // 공간 상세, 탐색 응답의 isWishlisted (로그인 사용자 기준)
    boolean existsByUserIdAndSpaceId(Long userId, Long spaceId);

    // 공간 탐색 목록 wishCount
    @Query("""
            select w.spaceId as spaceId, count(w.id) as wishCount
            from Wishlist w
            where w.spaceId in :spaceIds
            group by w.spaceId
            """)
    List<WishCountView> countBySpaceIds(@Param("spaceIds") List<Long> spaceIds);

    interface WishCountView {
        Long getSpaceId();
        Long getWishCount();
    }

    // 공간 탐색 목록 isWishlisted 배치 조회 (해당 유저가 찜한 spaceId만 반환)
    @Query("""
            select w.spaceId
            from Wishlist w
            where w.userId = :userId
              and w.spaceId in :spaceIds
            """)
    List<Long> findWishlistedSpaceIds(@Param("userId") Long userId, @Param("spaceIds") List<Long> spaceId);
}
