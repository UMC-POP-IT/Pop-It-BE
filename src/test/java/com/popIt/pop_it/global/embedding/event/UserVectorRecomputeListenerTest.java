package com.popIt.pop_it.global.embedding.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.global.embedding.service.UserVectorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserVectorRecomputeListenerTest {

    @Mock
    private UserVectorService userVectorService;
    @Mock
    private ScheduledExecutorService userVectorDebounceScheduler;

    @InjectMocks
    private UserVectorRecomputeListener listener;

    @Test
    void 이벤트를_받으면_지연_실행을_예약하고_실행되면_재계산한다() {
        given(userVectorDebounceScheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .willReturn(mock(ScheduledFuture.class));

        listener.onUserEngagement(new UserEngagementEvent(1L));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(userVectorDebounceScheduler).schedule(taskCaptor.capture(), anyLong(), any(TimeUnit.class));

        // 실제 스케줄러가 지연 후 태스크를 실행했다고 가정하고, 캡처한 Runnable을 직접 실행해본다
        taskCaptor.getValue().run();
        verify(userVectorService).recomputeUserVector(1L);
    }

    @Test
    void 같은_유저에게_연속으로_이벤트가_오면_이전_예약을_취소한다() {
        ScheduledFuture firstScheduled = mock(ScheduledFuture.class);
        given(userVectorDebounceScheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .willReturn(firstScheduled)
                .willReturn(mock(ScheduledFuture.class));

        listener.onUserEngagement(new UserEngagementEvent(1L));
        listener.onUserEngagement(new UserEngagementEvent(1L));

        // 두 번째 이벤트가 오면 첫 번째로 예약해둔 재계산은 취소하고 타이머를 다시 시작해야 한다
        verify(firstScheduled).cancel(false);
    }

    @Test
    void 취향_벡터_재계산이_실패해도_예외를_전파하지_않는다() {
        willThrow(new IllegalStateException("redis down")).given(userVectorService).recomputeUserVector(1L);
        given(userVectorDebounceScheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .willReturn(mock(ScheduledFuture.class));

        listener.onUserEngagement(new UserEngagementEvent(1L));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(userVectorDebounceScheduler).schedule(taskCaptor.capture(), anyLong(), any(TimeUnit.class));
        taskCaptor.getValue().run(); // 여기서 예외가 전파되면 테스트가 실패한다
    }
}
