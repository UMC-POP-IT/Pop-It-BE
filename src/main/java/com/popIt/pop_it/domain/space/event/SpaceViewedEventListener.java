package com.popIt.pop_it.domain.space.event;

import com.popIt.pop_it.domain.space.service.SpaceVisitLogService;
import com.popIt.pop_it.domain.user_activity.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 공간 상세 조회의 부가 기록을 조회 응답과 분리해서 처리
@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceViewedEventListener {

    private final SpaceVisitLogService spaceVisitLogService;
    private final UserActivityService userActivityService;

    @Async("viewLogTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSpaceViewed(SpaceViewedEvent event) {
        try {
            spaceVisitLogService.recordVisit(event.spaceId(), event.userId(), event.viewerMode());
        } catch (Exception e) {
            log.warn("공간(id={}) 방문 기록(UV) 실패 - userId={}", event.spaceId(), event.userId(), e);
        }

        try {
            userActivityService.recordView(event.spaceId(), event.userId(), event.viewerMode());
        } catch (Exception e) {
            log.warn("공간(id={}) 조회 이력(개인화) 기록 실패 - userId={}", event.spaceId(), event.userId(), e);
        }
    }
}
