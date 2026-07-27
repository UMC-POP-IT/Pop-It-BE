package com.popIt.pop_it.domain.user_activity.repository;

import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.entity.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // 조회 기록 누적 (이미 행이 있으면 카운트만 증가)
    @Modifying
    @Query("""
            update UserActivity ua
            set ua.viewCount = ua.viewCount + 1,
                ua.lastViewedAt = :now
            where ua.userId = :userId
                and ua.spaceId = :spaceId
                and ua.activityType = :activityType
            """)
    int incrementViewCount(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("activityType") ActivityType activityType,
            @Param("now") LocalDateTime now
    );

    // 공간별 조회수 배치 집계 (가동률 판정용)
    @Query("""
        select ua.spaceId as spaceId, sum(ua.viewCount) as viewCount
        from UserActivity ua
        where ua.activityType = :activityType
            and ua.spaceId in :spaceIds
            and ua.lastViewedAt >= :since
        group by ua.spaceId
        """)
    List<SpaceViewCount> sumViewCountBySpaceId(
            @Param("activityType") ActivityType activityType,
            @Param("spaceIds") List<Long> spaceIds,
            @Param("since") LocalDateTime since
    );
}
