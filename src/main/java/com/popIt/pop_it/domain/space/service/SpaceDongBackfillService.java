package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SpaceDongBackfillService {

    private final SpaceService spaceService;
    private final KakaoLocalService kakaoLocalService;
    private final SpaceRepository spaceRepository;

    // 공간 1개의 동을 다시 변환해 채우기
    // 스케줄러 전용, 독립 트랜젝션으로 처리 (한 건의 실패가 다른 건에 영향을 주자 않도록)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean backfillDong(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.FACILITY_NOT_FOUND));

        if (space.getDong() != null || space.getDeletedAt() != null) {
            return false;
        }

        Optional<String> dong = kakaoLocalService.resolveDong(space.getLatitude(), space.getLongitude());
        if (dong.isEmpty()) {
            return false;
        }

        space.applyDong(dong.get());
        spaceRepository.saveAndFlush(space);
        return true;
    }
}
