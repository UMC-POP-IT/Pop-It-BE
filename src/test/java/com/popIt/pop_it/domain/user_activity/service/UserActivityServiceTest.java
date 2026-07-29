package com.popIt.pop_it.domain.user_activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.repository.UserEventRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.event.UserEngagementEvent;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
        given(userActivityRepository.findByUserIdAndSpaceId(userId, spaceId)).willReturn(Optional.empty());

        userActivityService.recordView(userId, spaceId);

        ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
        verify(userActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getViewCount()).isEqualTo(1);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getSpaceId()).isEqualTo(spaceId);
    }

    @Test
    void 이미_조회한_적_있는_공간이면_조회수를_누적한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        UserActivity existing = UserActivity.builder()
                .userId(userId).spaceId(spaceId).viewCount(2).build();
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "합정동")));
        given(userActivityRepository.findByUserIdAndSpaceId(userId, spaceId)).willReturn(Optional.of(existing));

        userActivityService.recordView(userId, spaceId);

        verify(userActivityRepository).save(existing);
        assertThat(existing.getViewCount()).isEqualTo(3);
    }

    @Test
    void 조회_시점의_공간_동을_스냅샷으로_이벤트를_남긴다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "성수동")));
        given(userActivityRepository.findByUserIdAndSpaceId(userId, spaceId)).willReturn(Optional.empty());

        userActivityService.recordView(userId, spaceId);

        ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventRepository).save(captor.capture());
        assertThat(captor.getValue().getRegion()).isEqualTo("성수동");
    }

    @Test
    void 조회_기록_후_취향_벡터_재계산_이벤트를_발행한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.of(space(spaceId, "합정동")));
        given(userActivityRepository.findByUserIdAndSpaceId(userId, spaceId)).willReturn(Optional.empty());

        userActivityService.recordView(userId, spaceId);

        verify(eventPublisher).publishEvent(new UserEngagementEvent(userId));
    }

    @Test
    void 존재하지_않는_공간이면_예외가_발생한다() {
        Long userId = 1L;
        Long spaceId = 999L;
        given(spaceRepository.findById(spaceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userActivityService.recordView(userId, spaceId))
                .isInstanceOf(ProjectException.class);
        verify(userActivityRepository, never()).save(any());
    }

    private Space space(Long id, String dong) {
        return Space.builder().id(id).dong(dong).build();
    }
}
