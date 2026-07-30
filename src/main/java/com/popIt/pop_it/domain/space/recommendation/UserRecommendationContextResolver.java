package com.popIt.pop_it.domain.space.recommendation;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import com.popIt.pop_it.domain.user_event.repository.UserEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저의 최근 이벤트 이력을 집계해 RecommendationReasonEvaluator가 바로 쓸 수 있는
 * UserRecommendationContext를 만든다.
 *
 * REGION_PIVOT과 PRICE_TARGET이 둘 다 "최근 7일"을 기준으로 삼으므로,
 * 이벤트 조회는 한 번만 하고 대표 지역/평균가를 여기서 함께 계산해 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class UserRecommendationContextResolver {

    private final UserEventRepository userEventRepository;
    private final SpaceRepository spaceRepository;

    @Transactional(readOnly = true)
    public UserRecommendationContext resolve(Long userId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RecommendationReasonConstants.REGION_PIVOT_WINDOW_DAYS);
        List<UserEvent> recentEvents = userEventRepository.findByUserIdAndCreatedAtAfter(userId, cutoff);

        // 대표 지역은 조회(VIEW)뿐 아니라 찜(WISHLIST)까지 합산한 빈도로 정한다
        Map<String, Long> interactionCountByRegion = recentEvents.stream()
                .collect(Collectors.groupingBy(UserEvent::getRegion, Collectors.counting()));

        Map.Entry<String, Long> topRegion = interactionCountByRegion.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String representativeRegion = topRegion != null ? topRegion.getKey() : null;

        // REGION_PIVOT은 "찜했거나 3회 이상 조회"를 조회/찜 각각 따로 판단하므로 대표 지역의 조회 수와 찜 수를 나눠서 넘긴다
        long representativeRegionViewCount = countByRegionAndType(recentEvents, representativeRegion, UserEventType.VIEW);
        long representativeRegionWishlistCount = countByRegionAndType(recentEvents, representativeRegion, UserEventType.WISHLIST);

        // COLD_TARGET_NEARBY 판별용 - 조회/찜 유형 구분 없이 "가본 적 있는 동" 전체 집합
        Set<String> visitedDongs = recentEvents.stream()
                .map(UserEvent::getRegion)
                .collect(Collectors.toSet());

        Double recentInteractionAvgPrice = resolveAvgPriceOfInteractedSpaces(recentEvents);
        Double representativeRegionAvgPrice = representativeRegion == null
                ? null
                : spaceRepository.findAvgPricePerDayByDong(representativeRegion);

        return new UserRecommendationContext(
                userId,
                !recentEvents.isEmpty(),
                visitedDongs,
                representativeRegion,
                representativeRegionViewCount,
                representativeRegionWishlistCount,
                recentInteractionAvgPrice,
                representativeRegionAvgPrice
        );
    }

    private long countByRegionAndType(List<UserEvent> events, String region, UserEventType type) {
        if (region == null) {
            return 0L;
        }
        return events.stream()
                .filter(event -> event.getEventType() == type && region.equals(event.getRegion()))
                .count();
    }

    private Double resolveAvgPriceOfInteractedSpaces(List<UserEvent> recentEvents) {
        Set<Long> interactedSpaceIds = recentEvents.stream()
                .map(UserEvent::getSpaceId)
                .collect(Collectors.toSet());
        if (interactedSpaceIds.isEmpty()) {
            return null;
        }

        OptionalDouble average = spaceRepository.findAllById(interactedSpaceIds).stream()
                .mapToInt(Space::getPricePerDay)
                .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }
}
