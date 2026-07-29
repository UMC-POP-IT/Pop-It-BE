package com.popIt.pop_it.domain.user_activity.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.entity.enums.ActivityType;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import com.popIt.pop_it.domain.user_event.repository.UserEventRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.event.UserEngagementEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공간 조회를 AI 추천용 개인화 신호로 남긴다.
 * - UserActivity: 공간별 누적 조회수 (취향 벡터 계산 재료, UserVectorService 참고)
 * - UserEvent: 조회 시각 그대로 보존한 원본 로그 (추천 사유 태그 판별 재료, UserRecommendationContextResolver 참고)
 * 둘을 남긴 뒤 취향 벡터가 최신 이력을 반영하도록 재계산 이벤트를 발행한다 (찜 토글과 동일한 패턴).
 */
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final UserEventRepository userEventRepository;
    private final SpaceRepository spaceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordView(Long userId, Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        UserActivity activity = userActivityRepository.findByUserIdAndSpaceId(userId, spaceId)
                .orElseGet(() -> UserActivity.builder()
                        .userId(userId)
                        .spaceId(spaceId)
                        .activityType(ActivityType.VIEW)
                        .build());
        activity.recordView(); // viewCount 증가 + lastViewedAt 갱신 일괄 처리
        userActivityRepository.save(activity);

        if (space.getDong() != null) {
            userEventRepository.save(UserEvent.builder()
                    .userId(userId)
                    .spaceId(spaceId)
                    .eventType(UserEventType.VIEW)
                    .region(space.getDong())
                    .build());
        }

        eventPublisher.publishEvent(new UserEngagementEvent(userId));
    }
}
