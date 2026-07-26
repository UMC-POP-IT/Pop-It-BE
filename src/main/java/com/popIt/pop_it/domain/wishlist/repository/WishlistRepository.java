package com.popIt.pop_it.domain.wishlist.repository;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 공간 상세, 탐색 응답의 wishCount (로그인 여부와 상관 X)
    long countBySpaceId(Long spaceId);

    // 공간 상세, 탐색 응답의 isWishlisted (로그인 사용자 기준)
    boolean existsByUserIdAndSpaceId(Long userId, Long spaceId);

    // 찜 토글 해제 시 사용. 파생 삭제(select 후 id별 remove)는 동시 해제 시 이미 지워진 행에
    // ObjectOptimisticLockingFailureException을 던지므로 WHERE 절 벌크 삭제로 멱등 처리한다.
    // 서비스(toggle)는 hot key 락 보유시간을 줄이려 non-transactional이므로, 이 짧은 삭제가
    // 자체 트랜잭션을 갖도록 @Transactional을 직접 부여한다(즉시 커밋 → 락 즉시 해제).
    @Transactional
    @Modifying
    @Query("delete from Wishlist w where w.userId = :userId and w.spaceId = :spaceId")
    void deleteByUserIdAndSpaceId(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    // 내가 찜한 공간 목록 (최근 찜한 순). Wishlist-Space 세타 조인으로
    // 삭제된(soft delete) 공간은 제외하고, 정확한 페이징(count)까지 함께 계산한다.
    @Query(value = """
            select s from Wishlist w, Space s
            where w.spaceId = s.id
              and w.userId = :userId
              and s.deletedAt is null
            order by w.createdAt desc
            """,
            countQuery = """
            select count(s) from Wishlist w, Space s
            where w.spaceId = s.id
              and w.userId = :userId
              and s.deletedAt is null
            """)
    Page<Space> findWishlistedSpaces(@Param("userId") Long userId, Pageable pageable);

    // 여러 공간의 찜 수를 한 번의 쿼리로 집계 (목록 조회 시 N+1 방지)
    @Query("select w.spaceId as spaceId, count(w) as count from Wishlist w where w.spaceId in :spaceIds group by w.spaceId")
    List<WishCountBySpace> countBySpaceIds(@Param("spaceIds") List<Long> spaceIds);

    // 특정 유저가 찜한 spaceId 배치 조회 (탐색 목록 isWishlisted N+1 방지)
    @Query("""
            select w.spaceId
            from Wishlist w
            where w.userId = :userId
              and w.spaceId in :spaceIds
            """)
    List<Long> findWishlistedSpaceIds(@Param("userId") Long userId, @Param("spaceIds") List<Long> spaceIds);
}
