package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceVisitLog;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.space.repository.SpaceVisitLogRepository;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공간 UV(순방문자) 집계용 원본 방문 기록
 * (space_id, user_id, visit_date) 유니크 제약이 실제 카운팅 정확성을 보장
 * Sevice에서는 중복 저장을 피하기 위한 사전 체크 + 동시 요청 충돌 방어만 담당
 */
@Service
@RequiredArgsConstructor
public class SpaceVisitLogService {

    private final SpaceVisitLogRepository spaceVisitLogRepository;
    private final SpaceRepository spaceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVisit(Long spaceId, Long userId, UserMode mode) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        // 호스트 모드로 자기 공간을 조회한 경우는 UV 집계 대상이 아니므로 기록하지 않는다.
        if (mode == UserMode.HOST && userId.equals(space.getHostId())) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (spaceVisitLogRepository.existsBySpaceIdAndUserIdAndVisitDate(spaceId, userId, today)) {
            return;
        }
        try {
            spaceVisitLogRepository.saveAndFlush(SpaceVisitLog.builder()
                    .spaceId(spaceId)
                    .userId(userId)
                    .visitDate(today)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 유니크 제약 충돌 - 이미 오늘자 방문이 기록된 것이므로 무시
        }
    }
}
