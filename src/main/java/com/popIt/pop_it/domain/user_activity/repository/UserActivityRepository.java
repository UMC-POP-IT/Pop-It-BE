package com.popIt.pop_it.domain.user_activity.repository;

import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // AI 추천용 취향 벡터 계산 - 기간 제한 없이 전체 조회 이력 조회 (오래된 이력은 TimeDecayCalculator가 가중치로 반영)
    List<UserActivity> findByUserId(Long userId);

    // 공간 조회 기록 upsert용
    Optional<UserActivity> findByUserIdAndSpaceId(Long userId, Long spaceId);
}
