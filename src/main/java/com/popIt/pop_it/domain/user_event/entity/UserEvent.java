package com.popIt.pop_it.domain.user_event.entity;

import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 추천 사유 태그(REGION_PIVOT, PRICE_TARGET 등) 판별을 위한 유저 행동 원본 로그.
 * UserActivity(공간별 누적 조회수)와 달리 이벤트 하나하나의 시각과 지역(region)을 그대로 보존해서,
 * "최근 N일 내 A지역을 몇 번 봤는지" 같은 기간 기반 집계를 가능하게 한다.
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_event")
public class UserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserEventType eventType;

    @Column(nullable = false, length = 50)
    private String region; // 이벤트 발생 시점의 공간 지역(동) 스냅샷

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
