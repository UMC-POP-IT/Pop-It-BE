package com.popIt.pop_it.domain.space.recommendation;

/**
 * 추천 사유 태그 판별 결과 - API 응답에 그대로 매핑 가능한 구조.
 *
 * @param type           선택된 추천 사유 태그
 * @param mentText       노출용 최종 멘트 문자열
 * @param calculatedValue 태그 판별 과정에서 계산된 대표 수치(예: PRICE_TARGET/REGION_CHEAPER_NEARBY의 할인율 %). 해당 없으면 null
 */
public record RecommendationReasonResult(
        RecommendationReasonType type,
        String mentText,
        Double calculatedValue
) {
}
