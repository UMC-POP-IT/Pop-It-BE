package com.popIt.pop_it.global.embedding.event;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.global.embedding.service.UserVectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserVectorRecomputeListenerTest {

    @Mock
    private UserVectorService userVectorService;

    @InjectMocks
    private UserVectorRecomputeListener listener;

    @Test
    void 커밋_후_이벤트를_받으면_취향_벡터_재계산을_호출한다() {
        listener.onUserEngagement(new UserEngagementEvent(1L));

        verify(userVectorService).recomputeUserVector(1L);
    }

    @Test
    void 취향_벡터_재계산이_실패해도_예외를_전파하지_않는다() {
        willThrow(new IllegalStateException("redis down"))
                .given(userVectorService).recomputeUserVector(1L);

        listener.onUserEngagement(new UserEngagementEvent(1L));
    }
}
