package com.popIt.pop_it.global.embedding.event;

import com.popIt.pop_it.global.embedding.service.UserVectorService;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 찜/조회 이벤트가 커밋된 뒤 유저 취향 벡터를 재계산한다.
 * AFTER_COMMIT으로 처리해 재계산 실패가 원본 찜/조회 트랜잭션에 영향을 주지 않도록 격리한다.
 *
 * 같은 유저가 짧은 시간에 연속으로 찜/조회하면(연타 등) DEBOUNCE_DELAY만큼 시간이 지난 뒤
 * 마지막 이벤트 기준으로 딱 한 번만 재계산하도록 디바운스한다.
 * 새 이벤트가 오면 대기 중이던 재계산은 취소하고 타이머를 다시 시작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserVectorRecomputeListener {

    private static final Duration DEBOUNCE_DELAY = Duration.ofSeconds(2);

    private final UserVectorService userVectorService;
    private final ScheduledExecutorService userVectorDebounceScheduler;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pendingRecomputes = new ConcurrentHashMap<>();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserEngagement(UserEngagementEvent event) {
        Long userId = event.userId();

        AtomicReference<ScheduledFuture<?>> selfRef = new AtomicReference<>();
        ScheduledFuture<?> scheduled = userVectorDebounceScheduler.schedule(
                () -> recompute(userId, selfRef.get()), DEBOUNCE_DELAY.toMillis(), TimeUnit.MILLISECONDS);
        selfRef.set(scheduled);

        ScheduledFuture<?> previous = pendingRecomputes.put(userId, scheduled);
        if (previous != null) {
            previous.cancel(false); // 이미 실행 중이면 취소돼도 무시됨
        }
    }

    private void recompute(Long userId, ScheduledFuture<?> self) {
        // 맵에 남아있는 게 정확히 "나 자신"일 때만 지운다.
        // 본인이 더 이상 최신이 아니면(=제거 실패) 곧 실행될 최신 태스크에 맡기고 재계산도 건너뛴다.
        boolean stillCurrent = pendingRecomputes.remove(userId, self);
        if (!stillCurrent) {
            return;
        }
        try {
            userVectorService.recomputeUserVector(userId);
        } catch (Exception e) {
            log.warn("유저(id={}) 취향 벡터 재계산에 실패했습니다.", userId, e);
        }
    }
}
