package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.SpaceDailyUv;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceDailyUvRepository extends JpaRepository<SpaceDailyUv, Long> {

    Optional<SpaceDailyUv> findBySpaceIdAndVisitDate(Long spaceId, LocalDate visitDate);

    // 최대 보관 기간(8일)이 지난 집계 데이터 정리
    long deleteByVisitDateBefore(LocalDate cutoff);
}
