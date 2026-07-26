package com.popIt.pop_it.domain.space.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.SpaceVisitLog;
import com.popIt.pop_it.domain.space.repository.SpaceVisitLogRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SpaceVisitLogServiceTest {

    @Mock
    private SpaceVisitLogRepository spaceVisitLogRepository;

    @InjectMocks
    private SpaceVisitLogService spaceVisitLogService;

    @Test
    void 오늘_첫_방문이면_방문_기록을_저장한다() {
        Long spaceId = 1L;
        Long userId = 10L;
        given(spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, userId, LocalDate.now()))
                .willReturn(false);

        spaceVisitLogService.recordVisit(spaceId, userId);

        verify(spaceVisitLogRepository).saveAndFlush(any(SpaceVisitLog.class));
    }

    @Test
    void 오늘_이미_방문했으면_다시_저장하지_않는다() {
        Long spaceId = 1L;
        Long userId = 10L;
        given(spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, userId, LocalDate.now()))
                .willReturn(true);

        spaceVisitLogService.recordVisit(spaceId, userId);

        verify(spaceVisitLogRepository, never()).saveAndFlush(any(SpaceVisitLog.class));
    }

    @Test
    void 동시_요청으로_유니크_제약_충돌이_나도_예외를_전파하지_않는다() {
        Long spaceId = 1L;
        Long userId = 10L;
        given(spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, userId, LocalDate.now()))
                .willReturn(false);
        given(spaceVisitLogRepository.saveAndFlush(any(SpaceVisitLog.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        spaceVisitLogService.recordVisit(spaceId, userId);
    }
}
