package com.popIt.pop_it.domain.space.scheduler;

import com.popIt.pop_it.domain.space.entity.SpaceDailyUv;
import com.popIt.pop_it.domain.space.repository.SpaceDailyUvRepository;
import com.popIt.pop_it.domain.space.repository.SpaceVisitLogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전날 공간별 방문 로그(SpaceVisitLog)를 distinct count로 집계해 SpaceDailyUv에 확정 저장하고,
 * 처리 끝난 원본 로그와 보관 기간(8일)이 지난 집계 데이터를 정리하는 일일 배치.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceUvAggregationScheduler {

    private static final int RETENTION_DAYS = 8;

    private final SpaceVisitLogRepository spaceVisitLogRepository;
    private final SpaceDailyUvRepository spaceDailyUvRepository;
    private final Clock clock;

    @Scheduled(cron = "0 10 0 * * *") // 매일 00:10 - 전날 UV 확정
    @Transactional
    public void aggregateYesterdayUv() {
        // 배치 하나는 하나의 기준 시점을 가져야 하므로 today를 한 번만 계산해 yesterday/retentionCutoff를 파생시킨다.
        // (그 사이 조회~삭제 처리 중 자정을 넘겨도 두 값이 서로 어긋나지 않도록 함)
        LocalDate today = LocalDate.now(clock);
        LocalDate yesterday = today.minusDays(1);

        List<SpaceVisitLogRepository.SpaceUvCount> counts =
                spaceVisitLogRepository.countDistinctUsersByVisitDate(yesterday);

        for (SpaceVisitLogRepository.SpaceUvCount count : counts) {
            SpaceDailyUv daily = spaceDailyUvRepository
                    .findBySpaceIdAndVisitDate(count.getSpaceId(), yesterday)
                    .orElseGet(() -> SpaceDailyUv.builder()
                            .spaceId(count.getSpaceId())
                            .visitDate(yesterday)
                            .build());
            daily.updateUvCount(count.getUvCount().intValue());
            spaceDailyUvRepository.save(daily);
        }

        // 집계 끝난 원본 방문 로그 정리 - 실제 8일치 히스토리는 SpaceDailyUv가 담당.
        // 오늘 이전 전체가 아니라 방금 집계한 날짜(yesterday)만 지운다 - 배치가 못 돈 날이 있어도
        // 그 미집계 로그는 남아있어야 다음에 되살릴 여지가 있다.
        spaceVisitLogRepository.deleteByVisitDate(yesterday);

        // 보관 기간(8일)이 지난 집계 데이터 정리
        LocalDate retentionCutoff = today.minusDays(RETENTION_DAYS);
        spaceDailyUvRepository.deleteByVisitDateBefore(retentionCutoff);

        log.info("공간 UV 배치 완료 - 대상일: {}, 집계된 공간 수: {}", yesterday, counts.size());
    }
}
