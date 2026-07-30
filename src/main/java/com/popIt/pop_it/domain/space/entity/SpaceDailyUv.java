package com.popIt.pop_it.domain.space.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공간별 일간 순방문자(UV) 수 - SpaceUvAggregationScheduler가 매일 확정해서 저장한다.
 * 최대 8일치만 보관되며, 그보다 오래된 행은 배치가 정리한다.
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "space_daily_uv",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_space_daily_uv_space_date",
                columnNames = {"space_id", "visit_date"})
)
public class SpaceDailyUv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "uv_count", nullable = false)
    private int uvCount;

    public void updateUvCount(int uvCount) {
        this.uvCount = uvCount;
    }
}
