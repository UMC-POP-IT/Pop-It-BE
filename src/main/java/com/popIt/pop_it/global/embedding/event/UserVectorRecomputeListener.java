package com.popIt.pop_it.global.embedding.event;

import com.popIt.pop_it.global.embedding.service.UserVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 찜/조회 이벤트가 커밋된 뒤 유저 취향 벡터를 재계산한다.
 * AFTER_COMMIT으로 처리해 재계산 실패가 원본 찜/조회 트랜잭션에 영향을 주지 않도록 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserVectorRecomputeListener {

    private final UserVectorService userVectorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEngagement(UserEngagementEvent event) {
        try {
            userVectorService.recomputeUserVector(event.userId());
        } catch (Exception e) {
            log.warn("유저(id={}) 취향 벡터 재계산에 실패했습니다.", event.userId(), e);
        }
    }
}
