package com.popIt.pop_it.domain.user_event.repository;

import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    // 추천 사유 태그 판별용 - 최근 N일 이벤트 (REGION_PIVOT의 지역별 조회 횟수, PRICE_TARGET의 평균가 계산 재료)
    List<UserEvent> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    // 찜 해제 시 이벤트 삭제
    void deleteByUserIdAndSpaceIdAndEventType(Long userId, Long spaceId, UserEventType eventType);
}
