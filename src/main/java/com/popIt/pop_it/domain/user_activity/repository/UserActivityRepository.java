package com.popIt.pop_it.domain.user_activity.repository;

import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // AI 추천용 취향 벡터 계산 - 기간 제한 없이 전체 조회 이력 조회 (오래된 이력은 TimeDecayCalculator가 가중치로 반영)
    List<UserActivity> findByUserId(Long userId);

    // 공간 조회 기록 upsert용
    Optional<UserActivity> findByUserIdAndSpaceId(Long userId, Long spaceId);

    // 조회수 증가를 DB에서 원자적으로 처리
    @Modifying
    @Query("update UserActivity a set a.viewCount = a.viewCount + 1, a.lastViewedAt = :now "
            + "where a.userId = :userId and a.spaceId = :spaceId")
    int incrementViewCount(@Param("userId") Long userId, @Param("spaceId") Long spaceId, @Param("now") LocalDateTime now);

    // 첫 조회 insert가 유니크 제약 충돌로 실패한 뒤의 재시도 전용 - saveAndFlush 실패로 rollback-only가 된
    // 바깥 트랜잭션과 분리해 커밋되도록 별도의 REQUIRES_NEW 트랜잭션에서 실행한다.
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("update UserActivity a set a.viewCount = a.viewCount + 1, a.lastViewedAt = :now "
            + "where a.userId = :userId and a.spaceId = :spaceId")
    int incrementViewCountInNewTransaction(@Param("userId") Long userId, @Param("spaceId") Long spaceId, @Param("now") LocalDateTime now);
}
