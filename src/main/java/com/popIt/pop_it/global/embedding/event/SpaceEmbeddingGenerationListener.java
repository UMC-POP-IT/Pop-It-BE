package com.popIt.pop_it.global.embedding.event;

import com.popIt.pop_it.domain.space.service.SpaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 공간 등록 트랜잭션이 커밋된 뒤 임베딩을 생성한다.
 * AFTER_COMMIT으로 처리하는 이유: 공간 insert가 커밋되기 전에 별도 트랜잭션(REQUIRES_NEW)에서
 * findById를 하면 그 공간이 아직 안 보여서 SPACE_NOT_FOUND로 실패한다 - 커밋 이후에 실행돼야 조회가 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceEmbeddingGenerationListener {

    private final SpaceEmbeddingService spaceEmbeddingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSpaceCreated(SpaceCreatedEvent event) {
        log.info("공간(id={}) 임베딩 생성 이벤트 수신", event.spaceId());
        try {
            spaceEmbeddingService.generateAndSaveEmbedding(event.spaceId());
            log.info("공간(id={}) 임베딩 생성 완료", event.spaceId());
        } catch (Exception e) {
            log.warn("공간(id={}) 임베딩 생성에 실패했습니다.", event.spaceId(), e);
        }
    }
}
