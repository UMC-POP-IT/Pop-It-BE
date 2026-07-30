package com.popIt.pop_it.domain.space.recommendation;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지역명(동)으로 그 지역의 대표 중심 좌표를 구한다.
 * 별도 지역-좌표 마스터 테이블 없이, 해당 지역에 등록된 공간들의 좌표 평균(centroid)으로 계산한다.
 */
@Component
@RequiredArgsConstructor
public class RegionCenterResolver {

    private final SpaceRepository spaceRepository;

    public record RegionCenter(double latitude, double longitude) {}

    @Transactional(readOnly = true)
    public Optional<RegionCenter> resolveCenter(String region) {
        if (region == null || region.isBlank()) {
            return Optional.empty();
        }

        List<Space> spacesInRegion = spaceRepository.findAllByDongAndDeletedAtIsNull(region);
        if (spacesInRegion.isEmpty()) {
            return Optional.empty();
        }

        double avgLatitude = spacesInRegion.stream().mapToDouble(Space::getLatitude).average().orElseThrow();
        double avgLongitude = spacesInRegion.stream().mapToDouble(Space::getLongitude).average().orElseThrow();
        return Optional.of(new RegionCenter(avgLatitude, avgLongitude));
    }
}
