package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.space.converter.SpaceConverter;
import com.popIt.pop_it.domain.space.dto.AiRecommendationResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.recommendation.RecommendationMentTemplates;
import com.popIt.pop_it.domain.space.recommendation.RecommendationReasonEvaluator;
import com.popIt.pop_it.domain.space.recommendation.RecommendationReasonResult;
import com.popIt.pop_it.domain.space.recommendation.UserRecommendationContext;
import com.popIt.pop_it.domain.space.recommendation.UserRecommendationContextResolver;
import com.popIt.pop_it.domain.space.repository.SpaceEmbeddingRepository;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import com.popIt.pop_it.domain.wishlist.repository.WishCountBySpace;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.service.UserVectorService;
import com.popIt.pop_it.global.util.VectorMath;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 벡터와 공간 벡터 간 코사인 유사도로 정렬한 AI 추천 공간 목록을 조회한다.
 * 공간이 80개 규모라 별도 인덱스 없이 애플리케이션 레벨 순차 스캔으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceRecommendationService {

    // 이 미만이면 취향과 안 맞는다고 보고 추천에서 제외 (코사인 유사도 범위 -1~1)
    private static final double MIN_SIMILARITY_THRESHOLD = 0.5;

    private final SpaceEmbeddingRepository spaceEmbeddingRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final WishlistRepository wishlistRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserVectorService userVectorService;
    private final UserRecommendationContextResolver userRecommendationContextResolver;
    private final RecommendationReasonEvaluator recommendationReasonEvaluator;

    public AiRecommendationResDTO.AiRecommendedSpaceListRes getRecommendedSpaces(Long userId, String cursor, int size) {
        int offset = parseCursor(cursor);

        Optional<float[]> userVector = userVectorService.getUserVector(userId);
        if (userVector.isEmpty()) {
            // 찜/조회 이력이 전혀 없는 신규 유저는 취향 벡터가 없으므로 추천 없이 빈 목록을 반환.
            // hasActivityHistory=false로 "매칭되는 게 없어서 빈 것"과 구분해 프런트가 안내 문구를 보여줄 수 있게 한다.
            return AiRecommendationResDTO.AiRecommendedSpaceListRes.builder()
                    .spaces(List.of())
                    .hasNext(false)
                    .nextCursor(null)
                    .hasActivityHistory(false)
                    .build();
        }

        // 이미 찜했거나 조회한 공간은 추천의 의미가 없으므로 후보에서 제외
        Set<Long> excludedSpaceIds = wishlistRepository.findByUserId(userId).stream()
                .map(Wishlist::getSpaceId)
                .collect(Collectors.toSet());
        userActivityRepository.findByUserId(userId).stream()
                .map(UserActivity::getSpaceId)
                .forEach(excludedSpaceIds::add);
        List<Space> candidates = spaceEmbeddingRepository.findAllByEmbeddingIsNotNullAndDeletedAtIsNull().stream()
                .filter(space -> !excludedSpaceIds.contains(space.getId()))
                .toList();

        List<Space> ranked = rankBySimilarity(candidates, userVector.get());

        List<Space> page = ranked.stream().skip(offset).limit(size).toList();
        boolean hasNext = offset + page.size() < ranked.size();
        String nextCursor = hasNext ? String.valueOf(offset + page.size()) : null;

        List<Long> spaceIds = page.stream().map(Space::getId).toList();
        Map<Long, String> thumbnailBySpaceId = spaceImageRepository.findThumbnailsBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(image -> image.getSpace().getId(), SpaceImage::getImageUrl));
        Map<Long, Integer> wishCountBySpaceId = wishlistRepository.countBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(WishCountBySpace::getSpaceId, count -> count.getCount().intValue()));

        UserRecommendationContext recommendationContext = userRecommendationContextResolver.resolve(userId);

        // 후보 단계에서 찜한 공간을 이미 제외했으므로 isWishlisted는 항상 false
        List<AiRecommendationResDTO.AiRecommendedSpaceRes> spaces = page.stream()
                .map(space -> toAiRecommendedSpace(space, thumbnailBySpaceId, wishCountBySpaceId, recommendationContext))
                .toList();

        return AiRecommendationResDTO.AiRecommendedSpaceListRes.builder()
                .spaces(spaces)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .hasActivityHistory(true)
                .build();
    }

    private List<Space> rankBySimilarity(List<Space> candidates, float[] userVector) {
        return candidates.stream()
                .map(space -> Map.entry(space, VectorMath.cosineSimilarity(userVector, space.getEmbedding())))
                .filter(entry -> entry.getValue() >= MIN_SIMILARITY_THRESHOLD)
                .sorted(Map.Entry.<Space, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private int parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        int offset;
        try {
            offset = Integer.parseInt(cursor);
        } catch (NumberFormatException e) {
            throw new ProjectException(SpaceErrorCode.INVALID_CURSOR);
        }
        if (offset < 0) {
            throw new ProjectException(SpaceErrorCode.INVALID_CURSOR);
        }
        return offset;
    }

    private AiRecommendationResDTO.AiRecommendedSpaceRes toAiRecommendedSpace(Space space, Map<Long, String> thumbnailBySpaceId,
                                                                     Map<Long, Integer> wishCountBySpaceId,
                                                                     UserRecommendationContext recommendationContext) {
        String tag = recommendationReasonEvaluator.evaluate(space, recommendationContext)
                .map(RecommendationReasonResult::mentText)
                .orElse(RecommendationMentTemplates.DEFAULT);

        return AiRecommendationResDTO.AiRecommendedSpaceRes.builder()
                .spaceId(space.getId())
                .buildingName(space.getBuildingName())
                .tag(tag)
                .district(space.getDistrict())
                .roadAddress(space.getRoadAddress())
                .exclusiveArea(space.getExclusiveArea())
                .spaceCategory(space.getSpaceCategory().name())
                .keywords(SpaceConverter.toKeywords(space))
                .pricePerDay(space.getPricePerDay())
                .pricePerWeek(space.getPricePerDay() * 7)
                .pricePerMonth(space.getPricePerDay() * 30)
                .thumbnailUrl(thumbnailBySpaceId.get(space.getId()))
                .parkingAvailable(space.getParkingAvailable())
                .isWishlisted(false)
                .wishCount(wishCountBySpaceId.getOrDefault(space.getId(), 0))
                .build();
    }
}
