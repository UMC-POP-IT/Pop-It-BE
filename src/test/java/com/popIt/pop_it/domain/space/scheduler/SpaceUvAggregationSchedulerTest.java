package com.popIt.pop_it.domain.space.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.SpaceDailyUv;
import com.popIt.pop_it.domain.space.repository.SpaceDailyUvRepository;
import com.popIt.pop_it.domain.space.repository.SpaceVisitLogRepository;
import com.popIt.pop_it.domain.space.repository.SpaceVisitLogRepository.SpaceUvCount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpaceUvAggregationSchedulerTest {

    @Mock
    private SpaceVisitLogRepository spaceVisitLogRepository;
    @Mock
    private SpaceDailyUvRepository spaceDailyUvRepository;

    @InjectMocks
    private SpaceUvAggregationScheduler scheduler;

    @Test
    void 전날_방문_로그를_집계해_신규_UV_레코드를_생성한다() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SpaceUvCount count = mock(SpaceUvCount.class);
        given(count.getSpaceId()).willReturn(1L);
        given(count.getUvCount()).willReturn(3L);

        given(spaceVisitLogRepository.countDistinctUsersByVisitDate(yesterday)).willReturn(List.of(count));
        given(spaceDailyUvRepository.findBySpaceIdAndVisitDate(1L, yesterday)).willReturn(Optional.empty());

        scheduler.aggregateYesterdayUv();

        ArgumentCaptor<SpaceDailyUv> captor = ArgumentCaptor.forClass(SpaceDailyUv.class);
        verify(spaceDailyUvRepository).save(captor.capture());
        assertThat(captor.getValue().getSpaceId()).isEqualTo(1L);
        assertThat(captor.getValue().getVisitDate()).isEqualTo(yesterday);
        assertThat(captor.getValue().getUvCount()).isEqualTo(3);
    }

    @Test
    void 이미_집계된_UV_레코드가_있으면_값만_갱신한다() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SpaceUvCount count = mock(SpaceUvCount.class);
        given(count.getSpaceId()).willReturn(1L);
        given(count.getUvCount()).willReturn(5L);

        SpaceDailyUv existing = SpaceDailyUv.builder().spaceId(1L).visitDate(yesterday).uvCount(2).build();
        given(spaceVisitLogRepository.countDistinctUsersByVisitDate(yesterday)).willReturn(List.of(count));
        given(spaceDailyUvRepository.findBySpaceIdAndVisitDate(1L, yesterday)).willReturn(Optional.of(existing));

        scheduler.aggregateYesterdayUv();

        verify(spaceDailyUvRepository).save(existing);
        assertThat(existing.getUvCount()).isEqualTo(5);
    }

    @Test
    void 집계_끝난_원본_로그와_보관기간_지난_집계데이터를_정리한다() {
        given(spaceVisitLogRepository.countDistinctUsersByVisitDate(any())).willReturn(List.of());

        scheduler.aggregateYesterdayUv();

        verify(spaceVisitLogRepository).deleteByVisitDateBefore(eq(LocalDate.now()));
        verify(spaceDailyUvRepository).deleteByVisitDateBefore(eq(LocalDate.now().minusDays(8)));
    }
}
