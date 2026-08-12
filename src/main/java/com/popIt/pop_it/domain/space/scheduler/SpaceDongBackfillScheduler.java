package com.popIt.pop_it.domain.space.scheduler;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.space.service.KakaoLocalService;
import com.popIt.pop_it.domain.space.service.SpaceDongBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceDongBackfillScheduler {

    private final SpaceRepository spaceRepository;
    private final SpaceDongBackfillService spaceDongBackfillService;
    private final KakaoLocalService kakaoLocalService;
    private final Clock clock;

    private static final int RETRY_WINDOW_DAYS = 7;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void backfillMissingDong() {
        LocalDateTime createdAfter = LocalDateTime.now(clock).minusDays(RETRY_WINDOW_DAYS);

        List<Space> targets = spaceRepository.findTop50ByDongIsNullAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(createdAfter);

        if (targets.isEmpty()) {
            return;
        }

        int succeeded = 0;
        for (Space space : targets) {
            try {
                // 카카오 호출은 트랜잭션 밖에서 수행 (HTTP 대기 중 DB 커넥션, 락 점유 방지)
                Optional<String> dong = kakaoLocalService.resolveDong(space.getLatitude(), space.getLongitude());
                if (dong.isEmpty()) {
                    continue;
                }

                if (spaceDongBackfillService.applyDongIfStillMissing(
                        space.getId(), dong.get(), space.getLatitude(), space.getLongitude())
                ) {
                    succeeded++;
                }
            } catch (Exception e) {
                log.warn("동 백필 실패 - spaceId: {}", space.getId(), e);
            }
        }

        log.info("동 백필 완료 - 대상: {}건, 성공: {}건", targets.size(), succeeded);
    }
}
