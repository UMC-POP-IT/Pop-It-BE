package com.popIt.pop_it.domain.user_activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.repository.UserEventRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.event.UserEngagementEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository userActivityRepository;
    @Mock
    private UserEventRepository userEventRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserActivityService userActivityService;

    @Test
    void 처음_조회하는_공간이면_조회수_1로_새로_기록한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "합정동")));
        // UPDATE 대상 행이 없어서 0건 반영됐다고 가정 - 처음 조회하는 경우
        given(userActivityRepository.incrementViewCount(eq(userId), eq(spaceId), any())).willReturn(0);

        userActivityService.recordView(userId, spaceId, UserMode.GUEST);

        ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
        verify(userActivityRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getViewCount()).isEqualTo(1);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getSpaceId()).isEqualTo(spaceId);
    }

    @Test
    void 첫_조회가_동시에_겹쳐_유니크_제약에_걸리면_기존_행에_조회수를_반영한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "합정동")));
        // 둘 다 "처음 조회"로 보고 0을 반환했지만, insert 시점엔 이미 다른 요청이 행을 만들어놔서 유니크 제약 위반
        given(userActivityRepository.incrementViewCount(eq(userId), eq(spaceId), any())).willReturn(0);
        given(userActivityRepository.saveAndFlush(any(UserActivity.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        userActivityService.recordView(userId, spaceId, UserMode.GUEST);

        // insert 실패 후에는 그 행에 이번 조회분을 다시 반영해야 한다 (최초 1회 + 재시도 1회 = 총 2회 호출)
        verify(userActivityRepository, times(2))
                .incrementViewCount(eq(userId), eq(spaceId), any());
    }

    @Test
    void 이미_조회한_적_있는_공간이면_DB에서_원자적으로_조회수를_증가시키고_따로_저장하지_않는다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "합정동")));
        // UPDATE가 1건 반영됐다고 가정 - 이미 있던 행의 조회수가 DB에서 곧바로 +1 됨
        given(userActivityRepository.incrementViewCount(eq(userId), eq(spaceId), any())).willReturn(1);

        userActivityService.recordView(userId, spaceId, UserMode.GUEST);

        verify(userActivityRepository).incrementViewCount(eq(userId), eq(spaceId), any(LocalDateTime.class));
        // read-modify-write가 아니므로, 이미 있던 행에 대해서는 save()를 다시 호출하지 않는다
        verify(userActivityRepository, never()).save(any());
    }

    @Test
    void 조회_시점의_공간_동을_스냅샷으로_이벤트를_남긴다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "성수동")));
        given(userActivityRepository.incrementViewCount(eq(userId), eq(spaceId), any())).willReturn(1);

        userActivityService.recordView(userId, spaceId, UserMode.GUEST);

        ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventRepository).save(captor.capture());
        assertThat(captor.getValue().getRegion()).isEqualTo("성수동");
    }

    @Test
    void 조회_기록_후_취향_벡터_재계산_이벤트를_발행한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "합정동")));
        given(userActivityRepository.incrementViewCount(eq(userId), eq(spaceId), any())).willReturn(1);

        userActivityService.recordView(userId, spaceId, UserMode.GUEST);

        verify(eventPublisher).publishEvent(new UserEngagementEvent(userId));
    }

    @Test
    void 호스트_모드로_본인_공간을_조회하면_조회수를_기록하지_않는다() {
        Long hostId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, hostId, "합정동")));

        userActivityService.recordView(hostId, spaceId, UserMode.HOST);

        verify(userActivityRepository, never()).incrementViewCount(any(), any(), any());
        verify(userActivityRepository, never()).save(any());
        verify(userEventRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 게스트_모드면_본인_공간이어도_조회수를_기록한다() {
        Long hostId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, hostId, "합정동")));
        given(userActivityRepository.incrementViewCount(eq(hostId), eq(spaceId), any())).willReturn(1);

        userActivityService.recordView(hostId, spaceId, UserMode.GUEST);

        verify(userActivityRepository).incrementViewCount(eq(hostId), eq(spaceId), any());
    }

    @Test
    void 존재하지_않는_공간이면_예외가_발생한다() {
        Long userId = 1L;
        Long spaceId = 999L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userActivityService.recordView(userId, spaceId, UserMode.GUEST))
                .isInstanceOf(ProjectException.class);
        verify(userActivityRepository, never()).incrementViewCount(any(), any(), any());
        verify(userActivityRepository, never()).save(any());
    }

    private Space space(Long id, String dong) {
        return Space.builder().id(id).dong(dong).build();
    }

    private Space space(Long id, Long hostId, String dong) {
        return Space.builder().id(id).hostId(hostId).dong(dong).build();
    }
}
