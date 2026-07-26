package com.popIt.pop_it.global.embedding.event;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.service.SpaceEmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpaceEmbeddingGenerationListenerTest {

    @Mock
    private SpaceEmbeddingService spaceEmbeddingService;

    @InjectMocks
    private SpaceEmbeddingGenerationListener listener;

    @Test
    void 커밋_후_이벤트를_받으면_임베딩_생성을_호출한다() {
        listener.onSpaceCreated(new SpaceCreatedEvent(1L));

        verify(spaceEmbeddingService).generateAndSaveEmbedding(1L);
    }

    @Test
    void 임베딩_생성이_실패해도_예외를_전파하지_않는다() {
        willThrow(new IllegalStateException("gemini down"))
                .given(spaceEmbeddingService).generateAndSaveEmbedding(1L);

        listener.onSpaceCreated(new SpaceCreatedEvent(1L));
    }
}
