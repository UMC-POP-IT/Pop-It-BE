package com.popIt.pop_it.domain.space.recommendation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 추천 사유 태그. priority는 숫자가 작을수록 우선순위가 높다 -
 * 한 공간이 여러 태그 조건을 동시에 만족하면 priority가 가장 작은 태그 하나만 선택한다.
 */
@Getter
@RequiredArgsConstructor
public enum RecommendationReasonType {
    COLD_TARGET_NEARBY(1),
    REGION_PIVOT(2),
    FOMO(3),
    PRICE_TARGET(4),
    REGION_CHEAPER_NEARBY(5),
    ;

    private final int priority;
}
