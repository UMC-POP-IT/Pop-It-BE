package com.popIt.pop_it.domain.user_event.repository;

import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    // 추천 사유 태그 판별용 - 최근 N일 이벤트 (REGION_PIVOT의 지역별 조회 횟수, PRICE_TARGET의 평균가 계산 재료)
    List<UserEvent> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    // 찜 해제 시 이벤트 삭제
    @Transactional
    @Modifying
    @Query("delete from UserEvent e where e.userId = :userId and e.spaceId = :spaceId "
            + "and e.eventType = :eventType "
            + "and not exists (select 1 from Wishlist w where w.userId = :userId and w.spaceId = :spaceId)")
    void deleteByUserIdAndSpaceIdAndEventTypeIfNotWishlisted(
            @Param("userId") Long userId, @Param("spaceId") Long spaceId, @Param("eventType") UserEventType eventType);
}
