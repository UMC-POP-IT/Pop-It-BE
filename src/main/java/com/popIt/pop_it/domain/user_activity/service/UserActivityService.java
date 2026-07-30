package com.popIt.pop_it.domain.user_activity.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.entity.enums.ActivityType;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import com.popIt.pop_it.domain.user_event.repository.UserEventRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.event.UserEngagementEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
    public void recordView(Long userId, Long spaceId, UserMode mode) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        // 호스트 모드로 자기 공간을 조회한 경우는 기록하지 않는다.
        if (mode == UserMode.HOST && userId.equals(space.getHostId())) {
            return;
        }

        // DB에서 원자적으로 +1 - 동시 조회 시 lost update 방지
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = userActivityRepository.incrementViewCount(userId, spaceId, now);
        if (updatedRows == 0) {
            // 이 공간을 처음 조회하는 경우라 UPDATE 대상 행이 없었으므로 새로 만든다.
            try {
                userActivityRepository.saveAndFlush(UserActivity.builder()
                        .userId(userId)
                        .spaceId(spaceId)
                        .activityType(ActivityType.VIEW)
                        .viewCount(1)
                        .lastViewedAt(now)
                        .build());
            } catch (DataIntegrityViolationException e) {
                // 첫 조회가 동시에 겹쳐 다른 요청이 먼저 행을 만든 경우 +1을 반영
                userActivityRepository.incrementViewCount(userId, spaceId, now);
            }
        }

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
