package com.popIt.pop_it.domain.space.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceVisitLog;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.space.repository.SpaceVisitLogRepository;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class SpaceVisitLogServiceTest {

    @Mock
    private SpaceVisitLogRepository spaceVisitLogRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Spy
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    @InjectMocks
    private SpaceVisitLogService spaceVisitLogService;

    @Test
    void 오늘_첫_방문이면_방문_기록을_저장한다() {
        Long spaceId = 1L;
        Long userId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, 999L)));
        given(spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, userId, LocalDate.now(clock)))
                .willReturn(false);

        spaceVisitLogService.recordVisit(spaceId, userId, UserMode.GUEST);

        verify(spaceVisitLogRepository).saveAndFlush(any(SpaceVisitLog.class));
    }

    @Test
    void 오늘_이미_방문했으면_다시_저장하지_않는다() {
        Long spaceId = 1L;
        Long userId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, 999L)));
        given(spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, userId, LocalDate.now(clock)))
                .willReturn(true);

        spaceVisitLogService.recordVisit(spaceId, userId, UserMode.GUEST);

        verify(spaceVisitLogRepository, never()).saveAndFlush(any(SpaceVisitLog.class));
    }

    @Test
    void 동시_요청으로_유니크_제약_충돌이_나도_예외를_전파하지_않는다() {
        Long spaceId = 1L;
        Long userId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, 999L)));
        given(spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, userId, LocalDate.now(clock)))
                .willReturn(false);
        given(spaceVisitLogRepository.saveAndFlush(any(SpaceVisitLog.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        spaceVisitLogService.recordVisit(spaceId, userId, UserMode.GUEST);
    }

    @Test
    void 호스트_모드로_본인_공간을_방문하면_방문_기록을_남기지_않는다() {
        Long hostId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, hostId)));

        spaceVisitLogService.recordVisit(spaceId, hostId, UserMode.HOST);

        verify(spaceVisitLogRepository, never()).existsBySpaceIdAndUserIdAndVisitDate(any(), any(), any());
        verify(spaceVisitLogRepository, never()).saveAndFlush(any());
    }

    @Test
    void 게스트_모드면_본인_공간이어도_방문_기록을_남긴다() {
        Long hostId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, hostId)));
        given(spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, hostId, LocalDate.now(clock)))
                .willReturn(false);

        spaceVisitLogService.recordVisit(spaceId, hostId, UserMode.GUEST);

        verify(spaceVisitLogRepository).saveAndFlush(any(SpaceVisitLog.class));
    }

    private Space space(Long id, Long hostId) {
        return Space.builder().id(id).hostId(hostId).build();
    }
}
