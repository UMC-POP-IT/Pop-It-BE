package com.popIt.pop_it.domain.space.recommendation;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceDailyUv;
import com.popIt.pop_it.domain.space.recommendation.RegionCenterResolver.RegionCenter;
import com.popIt.pop_it.domain.space.repository.SpaceDailyUvRepository;
import com.popIt.pop_it.global.util.DistanceCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 후보 공간 하나와 유저 컨텍스트를 받아, 만족하는 추천 사유 태그를 판별한다.
 * 여러 태그 조건이 동시에 만족되면 priority가 가장 높은(숫자가 작은) 태그 하나만 선택한다.
 * 조건 판별만 담당하고 문구 조합은 RecommendationMentComposer에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class RecommendationReasonEvaluator {

    private final SpaceDailyUvRepository spaceDailyUvRepository;
    private final RegionCenterResolver regionCenterResolver;
    private final RecommendationMentComposer mentComposer;

    @Transactional(readOnly = true)
    public Optional<RecommendationReasonResult> evaluate(Space candidate, UserRecommendationContext context) {
        List<RecommendationReasonResult> satisfied = new ArrayList<>();

        evaluateColdTargetNearby(candidate, context).ifPresent(satisfied::add);
        evaluateRegionPivot(candidate, context).ifPresent(satisfied::add);
        evaluateFomo(candidate, context).ifPresent(satisfied::add);
        evaluatePriceTarget(candidate, context).ifPresent(satisfied::add);
        evaluateRegionCheaperNearby(candidate, context).ifPresent(satisfied::add);

        return satisfied.stream().min(Comparator.comparingInt(result -> result.type().getPriority()));
    }

    // 1순위: 후보 공간의 동(dong)이 유저의 최근 조회/찜 이력에 아예 없을 때(=아직 안 가본 동) 추천
    // (단, 이 태그와 FOMO는 본래 유저 개인 이력과 무관하게 판단될 수 있는 조건이라
    //  hasRecentActivity 가드를 추가로 건다 - 최소한 뭔가 조회/찜은 한 적 있어야 노출)
    private Optional<RecommendationReasonResult> evaluateColdTargetNearby(Space candidate, UserRecommendationContext context) {
        if (!context.hasRecentActivity()) {
            return Optional.empty(); // 개인화 신호(조회/찜)가 전혀 없는 유저에게는 추천하지 않음
        }
        if (context.visitedDongs().contains(candidate.getDong())) {
            return Optional.empty(); // 이미 조회/찜해본 적 있는 동이면 "아직 안 가본 동" 추천이 아님
        }

        String mentText = mentComposer.composeColdTargetNearby(context.representativeRegion());
        return Optional.of(new RecommendationReasonResult(RecommendationReasonType.COLD_TARGET_NEARBY, mentText, null));
    }

    // 2순위: 유저가 최근 N일간 찜했거나 3회 이상 반복 조회한 A지역 대신, 반경 내 다른 지역의 평점 높은 공간을 추천
    private Optional<RecommendationReasonResult> evaluateRegionPivot(Space candidate, UserRecommendationContext context) {
        String region = context.representativeRegion();
        boolean wishlisted = context.representativeRegionWishlistCount() >= 1;
        boolean repeatedlyViewed = context.representativeRegionViewCount() >= RecommendationReasonConstants.REGION_PIVOT_MIN_VIEW_COUNT;
        if (region == null || !(wishlisted || repeatedlyViewed)) {
            return Optional.empty();
        }
        if (region.equals(candidate.getDong())) {
            return Optional.empty(); // "대신"이므로 같은 지역 후보는 제외
        }

        Optional<RegionCenter> center = regionCenterResolver.resolveCenter(region);
        if (center.isEmpty() || candidate.getLatitude() == null || candidate.getLongitude() == null) {
            return Optional.empty();
        }

        double distanceKm = DistanceCalculator.haversineKm(
                center.get().latitude(), center.get().longitude(),
                candidate.getLatitude(), candidate.getLongitude());
        if (distanceKm > RecommendationReasonConstants.REGION_PIVOT_RADIUS_KM) {
            return Optional.empty();
        }

        String mentText = mentComposer.composeRegionPivot(region);
        return Optional.of(new RecommendationReasonResult(RecommendationReasonType.REGION_PIVOT, mentText, null));
    }

    // 3순위: 최근 24시간 UV가 충분히 많고, 직전 7일 일평균 대비 급등한 "핫한" 공간
    // SpaceUvAggregationScheduler가 채우는 SpaceDailyUv(최대 8일치 보관)에서 바로 계산한다 -
    // 최신 1일치가 "최근 24시간", 그 이전 최대 7일치가 "직전 7일 베이스라인"이 된다.
    private Optional<RecommendationReasonResult> evaluateFomo(Space candidate, UserRecommendationContext context) {
        if (!context.hasRecentActivity()) {
            return Optional.empty(); // 개인화 신호(조회/찜)가 전혀 없는 유저에게는 추천하지 않음
        }

        List<SpaceDailyUv> recentDailyUv = spaceDailyUvRepository.findTop8BySpaceIdOrderByVisitDateDesc(candidate.getId());
        if (recentDailyUv.size() < 2) {
            return Optional.empty(); // 비교할 베이스라인이 없으면 급증 여부를 판단할 수 없음
        }

        int uv24h = recentDailyUv.get(0).getUvCount();
        if (uv24h < RecommendationReasonConstants.FOMO_MIN_UV_24H) {
            return Optional.empty();
        }

        List<SpaceDailyUv> baselineDays = recentDailyUv.subList(1, recentDailyUv.size());
        double avgDailyUv7d = baselineDays.stream().mapToInt(SpaceDailyUv::getUvCount).average().orElse(0);
        if (avgDailyUv7d <= 0) {
            return Optional.empty();
        }

        double spikeRatio = uv24h / avgDailyUv7d;
        if (spikeRatio < RecommendationReasonConstants.FOMO_UV_SPIKE_RATIO) {
            return Optional.empty();
        }

        String mentText = mentComposer.composeFomo();
        return Optional.of(new RecommendationReasonResult(RecommendationReasonType.FOMO, mentText, null));
    }

    // 4순위: 유저가 최근 조회/찜한 공간들의 평균가 대비 낮거나 비슷한 가격의 공간을 추천
    private Optional<RecommendationReasonResult> evaluatePriceTarget(Space candidate, UserRecommendationContext context) {
        Double avgPrice = context.recentInteractionAvgPrice();
        if (avgPrice == null || avgPrice <= 0 || candidate.getPricePerDay() == null) {
            return Optional.empty();
        }

        double discountPercent = (avgPrice - candidate.getPricePerDay()) / avgPrice * 100.0;
        // "낮거나 비슷할 때" - 후보가 평균보다 유의미하게 비싸면(임계치보다 더 마이너스면) 대상에서 제외
        if (discountPercent < -RecommendationReasonConstants.PRICE_TARGET_SIMILAR_THRESHOLD_PERCENT) {
            return Optional.empty();
        }

        String region = context.representativeRegion();
        String mentText = mentComposer.composePriceTarget(region, discountPercent, RecommendationReasonConstants.PRICE_TARGET_SIMILAR_THRESHOLD_PERCENT);
        Double calculatedValue = discountPercent >= RecommendationReasonConstants.PRICE_TARGET_SIMILAR_THRESHOLD_PERCENT ? discountPercent : null;
        return Optional.of(new RecommendationReasonResult(RecommendationReasonType.PRICE_TARGET, mentText, calculatedValue));
    }

    // 5순위: 유저의 대표 지역(A) 평균가 대비 15~20% 이상 저렴한 공간을, A지역 반경 내 다른 지역에서 추천
    private Optional<RecommendationReasonResult> evaluateRegionCheaperNearby(Space candidate, UserRecommendationContext context) {
        String region = context.representativeRegion();
        Double regionAvgPrice = context.representativeRegionAvgPrice();
        if (region == null || regionAvgPrice == null || regionAvgPrice <= 0 || candidate.getPricePerDay() == null) {
            return Optional.empty();
        }
        if (region.equals(candidate.getDong())) {
            return Optional.empty(); // "근처"이므로 같은 지역 후보는 제외
        }

        double discountPercent = (regionAvgPrice - candidate.getPricePerDay()) / regionAvgPrice * 100.0;
        if (discountPercent < RecommendationReasonConstants.REGION_CHEAPER_MIN_DISCOUNT_PERCENT) {
            return Optional.empty();
        }

        Optional<RegionCenter> center = regionCenterResolver.resolveCenter(region);
        if (center.isEmpty() || candidate.getLatitude() == null || candidate.getLongitude() == null) {
            return Optional.empty();
        }

        double distanceKm = DistanceCalculator.haversineKm(
                center.get().latitude(), center.get().longitude(),
                candidate.getLatitude(), candidate.getLongitude());
        if (distanceKm > RecommendationReasonConstants.REGION_CHEAPER_RADIUS_KM) {
            return Optional.empty();
        }

        String mentText = mentComposer.composeRegionCheaperNearby(region);
        return Optional.of(new RecommendationReasonResult(RecommendationReasonType.REGION_CHEAPER_NEARBY, mentText, discountPercent));
    }
}
