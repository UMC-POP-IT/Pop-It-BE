package com.popIt.pop_it.domain.space.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 공간의 일별 순방문자(UV)를 정확히 세기 위한 원본 방문 기록.
 * (space_id, user_id, visit_date) 유니크 제약으로 같은 유저의 하루 재방문은 한 건으로만 잡힌다.
 * 배치가 집계한 뒤에는 삭제되므로 오래 보관되지 않는다 (실제 8일치 데이터는 SpaceDailyUv에 쌓인다).
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "space_visit_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_space_visit_log_space_user_date",
                columnNames = {"space_id", "user_id", "visit_date"})
)
public class SpaceVisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
