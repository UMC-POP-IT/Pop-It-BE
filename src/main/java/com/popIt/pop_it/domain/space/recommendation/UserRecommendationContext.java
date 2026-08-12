package com.popIt.pop_it.domain.space.recommendation;

import java.util.Set;

/**
 * 추천 사유 태그 판별에 필요한, 유저별로 한 번만 계산해서 재사용하는 컨텍스트.
 * 후보 공간 여러 개를 평가할 때마다 매번 이벤트를 다시 집계하지 않도록 미리 계산된 값들로 구성한다.
 *
 * @param hasRecentActivity               최근 N일 내 조회/찜 이벤트가 하나라도 있는지 - COLD_TARGET_NEARBY/FOMO는 개인화 신호가
 *                                        전혀 없는 유저에게는 뜨면 안 되므로 이 값이 false면 둘 다 무조건 제외한다
 * @param visitedDongs                    최근 N일 내 조회 또는 찜한 공간들의 동(dong) 집합 - COLD_TARGET_NEARBY 판별용.
 *                                        후보 공간의 동이 이 집합에 없으면 "아직 안 가본 동"으로 취급한다
 * @param representativeRegion            최근 N일 내 조회+찜 합산 빈도 1위 지역 - REGION_PIVOT/PRICE_TARGET/REGION_CHEAPER_NEARBY의 "A". 이력이 없으면 null
 * @param representativeRegionViewCount   representativeRegion의 최근 N일 VIEW 횟수 (찜 제외)
 * @param representativeRegionWishlistCount representativeRegion의 최근 N일 WISHLIST 횟수 - REGION_PIVOT은 "찜했거나 3회 이상 조회"를 OR로 판단하므로 조회/찜을 따로 갖고 있어야 한다
 * @param representativeRegionInteractionAvgPrice 최근 N일 내 조회/찜한 공간 중 representativeRegion에 속한 공간들의 평균 대관료 -
 *                                        PRICE_TARGET용. representativeRegion과 같은 지역 기준으로 계산해야 사유 문구의 지역명과 퍼센트가 일치한다. 이력이 없으면 null
 * @param representativeRegionAvgPrice    representativeRegion 전체 공간의 평균 대관료 - REGION_CHEAPER_NEARBY 기준가. 계산 불가하면 null
 */
public record UserRecommendationContext(
        Long userId,
        boolean hasRecentActivity,
        Set<String> visitedDongs,
        String representativeRegion,
        long representativeRegionViewCount,
        long representativeRegionWishlistCount,
        Double representativeRegionInteractionAvgPrice,
        Double representativeRegionAvgPrice
) {
}
