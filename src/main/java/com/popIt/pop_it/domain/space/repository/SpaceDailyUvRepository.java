package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.SpaceDailyUv;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceDailyUvRepository extends JpaRepository<SpaceDailyUv, Long> {

    Optional<SpaceDailyUv> findBySpaceIdAndVisitDate(Long spaceId, LocalDate visitDate);

    // FOMO 태그 판별용 - 최신순으로 최대 8일치(최근 1일 + 직전 7일 베이스라인)를 가져온다.
    // 보관 기간 자체가 8일이라 Top8이면 사실상 전체 이력과 같다.
    List<SpaceDailyUv> findTop8BySpaceIdOrderByVisitDateDesc(Long spaceId);

    // 최대 보관 기간(8일)이 지난 집계 데이터 정리
    long deleteByVisitDateBefore(LocalDate cutoff);
}
