package com.popIt.pop_it.domain.space.recommendation;

/**
 * 추천 사유 태그별 노출 멘트 문자열을 한 곳에 모아둔다 - 문구 수정 시 여기만 건드리면 된다.
 * "%s"/"[dong]" 자리는 전부 Space.dong이 채워진다 (구가 아니라 동 단위 - RecommendationReasonEvaluator 참고).
 * 태그 전용 문구를 만들 수 없는 경우(지역명 없음, 만족하는 태그 자체가 없음)는 전부 DEFAULT 하나로 통일한다.
 *
 * priority | 태그                    | 멘트
 * 1        | COLD_TARGET_NEARBY      | [dong]과 가까운 추천 공간
 * 2        | REGION_PIVOT            | [dong] 대신 여기 어때요?
 * 3        | FOMO                    | 다른 사장님들 문의 급증 / 최근 가장 핫한 공간이에요 (랜덤 선택)
 * 4        | PRICE_TARGET            | 최근 본 [dong] 공간보다 N% 저렴해요 / 찾고 계신 예산대에 딱 맞는 공간! (할인율 기준 분기)
 * 5        | REGION_CHEAPER_NEARBY   | [dong] 근처 가성비 공간
 */
public final class RecommendationMentTemplates {

    private RecommendationMentTemplates() {}

    // 1순위: COLD_TARGET_NEARBY
    public static final String COLD_TARGET_NEARBY = "%s 가까운 추천 공간"; // %s = "[동]와/과"처럼 조사까지 합쳐진 문자열
    public static final String COLD_TARGET_NEARBY_NO_REGION = "관심 지역과 가까운 추천 공간";

    // 2순위: REGION_PIVOT
    public static final String REGION_PIVOT = "%s 대신 여기 어때요?";

    // 3순위: FOMO (두 문구 중 랜덤 선택 - RecommendationMentComposer 참고)
    public static final String FOMO_INQUIRY_SURGE = "다른 사장님들 문의 급증";
    public static final String FOMO_HOTTEST = "최근 가장 핫한 공간이에요";

    // 4순위: PRICE_TARGET (할인율이 기준 이상이면 DISCOUNT, 아니면 BUDGET_FIT)
    public static final String PRICE_TARGET_DISCOUNT = "최근 본 %s 공간보다 %d%% 저렴해요";
    public static final String PRICE_TARGET_BUDGET_FIT = "찾고 계신 예산대에 딱 맞는 공간!";

    // 5순위: REGION_CHEAPER_NEARBY
    public static final String REGION_CHEAPER_NEARBY = "%s 근처 가성비 공간";

    // 어떤 태그 조건도 만족하지 못했을 때(또는 지역명이 없어 태그 전용 문구를 만들 수 없을 때) 쓰는 공통 기본 멘트
    public static final String DEFAULT = "관심 있는 공간과 비슷해요";
}
