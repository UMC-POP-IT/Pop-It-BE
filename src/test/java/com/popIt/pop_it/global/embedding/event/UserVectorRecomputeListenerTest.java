package com.popIt.pop_it.global.embedding.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void 스케줄된_직후_future가_아직_대입되기_전에_실행돼도_재계산이_유실되지_않는다() {
        // schedule()이 future를 반환하기도 전에(=future 필드가 채워지기 전에) 태스크가 곧바로
        // 실행되는 극단적인 상황을 재현한다 - "자기 자신"을 future 값으로 판별했다면 이 시점엔
        // 아직 null이라 ConcurrentHashMap.remove(key, null)이 항상 false를 반환해 재계산 자체가
        // 조용히 유실됐을 것이다. holder 객체 자체로 판별하므로 이 타이밍과 무관해야 한다.
        given(userVectorDebounceScheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .willAnswer(invocation -> {
                    Runnable task = invocation.getArgument(0);
                    task.run();
                    return mock(ScheduledFuture.class);
                });

        listener.onUserEngagement(new UserEngagementEvent(1L));

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
    void 취소가_실패해서_이전_태스크가_실행중이어도_최신_예약의_재계산만_실행된다() {
        ScheduledFuture firstScheduled = mock(ScheduledFuture.class);
        ScheduledFuture secondScheduled = mock(ScheduledFuture.class);
        given(userVectorDebounceScheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .willReturn(firstScheduled)
                .willReturn(secondScheduled);
        // 이미 실행이 시작돼서 취소 요청이 반영되지 않는 상황을 재현
        given(firstScheduled.cancel(false)).willReturn(false);

        listener.onUserEngagement(new UserEngagementEvent(1L));
        listener.onUserEngagement(new UserEngagementEvent(1L)); // 두 번째 이벤트가 대기 목록을 secondScheduled로 덮어씀

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(userVectorDebounceScheduler, times(2)).schedule(taskCaptor.capture(), anyLong(), any(TimeUnit.class));
        Runnable firstTask = taskCaptor.getAllValues().get(0);
        Runnable secondTask = taskCaptor.getAllValues().get(1);

        // 취소가 안 먹혀서 이미 실행 중이던 첫 번째 태스크가 뒤늦게 도는 상황을 재현
        firstTask.run();
        // 대기 목록의 현재 값은 이미 secondScheduled로 바뀌어 있으므로, 첫 번째 태스크는 자기 자신이
        // 아니라는 걸 확인하고 재계산을 건너뛰어야 한다 (안 그러면 이 자리에서 secondScheduled의
        // 엔트리를 잘못 지워서 취소 체인이 끊긴다)
        verify(userVectorService, never()).recomputeUserVector(1L);

        // 진짜 최신 태스크가 실행되면 그때는 정상적으로 재계산해야 한다
        secondTask.run();
        verify(userVectorService).recomputeUserVector(1L);
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
