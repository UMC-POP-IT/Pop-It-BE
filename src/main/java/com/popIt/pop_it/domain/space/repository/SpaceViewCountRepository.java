package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// 가동률 판정에 필요한 공간별 조회수만 읽는 리포지토리
public interface SpaceViewCountRepository extends JpaRepository<UserEvent, Long> {

    @Query("""
            select e.spaceId as spaceId, count(e) as viewCount
            from UserEvent e
            where e.eventType = :eventType
              and e.spaceId in :spaceIds
              and e.createdAt >= :since
            group by e.spaceId
            """)
    List<SpaceViewCount> countViewsBySpaceIds(
            @Param("eventType")UserEventType eventType,
            @Param("spaceIds") List<Long> spaceIds,
            @Param("since") LocalDateTime since
    );
}
