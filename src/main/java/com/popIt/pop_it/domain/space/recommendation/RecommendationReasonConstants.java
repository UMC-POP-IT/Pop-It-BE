package com.popIt.pop_it.domain.space.recommendation;

/**
 * 추천 사유 태그 판별에 쓰이는 매직넘버 모음.
 */
public final class RecommendationReasonConstants {

    private RecommendationReasonConstants() {}

    // 최근 N일 내 A지역 조회+찜 합산 3회 이상
    public static final int REGION_PIVOT_MIN_VIEW_COUNT = 3;
    public static final int REGION_PIVOT_WINDOW_DAYS = 7;
    public static final double REGION_PIVOT_RADIUS_KM = 3.0;
    public static final int FOMO_MIN_UV_24H = 15;
    // 직전 7일 일평균 UV 대비 150%
    public static final double FOMO_UV_SPIKE_RATIO = 1.5;
    public static final int PRICE_TARGET_WINDOW_DAYS = 7;
    // 이 % 미만 차이면 "비슷한 가격"으로 취급
    public static final double PRICE_TARGET_SIMILAR_THRESHOLD_PERCENT = 3.0;
    public static final double REGION_CHEAPER_RADIUS_KM = 3.0;
    public static final double REGION_CHEAPER_MIN_DISCOUNT_PERCENT = 15.0;
}
