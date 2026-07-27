package com.popIt.pop_it.domain.space.recommendation;

import com.popIt.pop_it.global.util.KoreanParticleUtil;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * 추천 사유 태그와 판별 과정에서 계산된 값(지역명, 할인율 등)을 받아 최종 노출 멘트를 조합한다.
 * 조건 판별(RecommendationReasonEvaluator)과 문구 생성 책임을 분리하기 위한 클래스.
 */
@Component
public class RecommendationMentComposer {

    // FOMO는 멘트 후보가 2개뿐이고 우선순위 차이가 없는 문구라, 매번 같은 문구만 노출돼 식상해지는 걸
    // 막기 위해 고정 우선순위 대신 랜덤 선택을 쓴다.
    private static final List<String> FOMO_MENTS = List.of(
            RecommendationMentTemplates.FOMO_INQUIRY_SURGE,
            RecommendationMentTemplates.FOMO_HOTTEST
    );

    public String composeColdTargetNearby(String region) {
        if (isBlank(region)) {
            return RecommendationMentTemplates.COLD_TARGET_NEARBY_NO_REGION;
        }
        String regionWithParticle = region + KoreanParticleUtil.waGwa(region);
        return RecommendationMentTemplates.COLD_TARGET_NEARBY.formatted(regionWithParticle);
    }

    public String composeRegionPivot(String region) {
        return isBlank(region)
                ? RecommendationMentTemplates.DEFAULT
                : RecommendationMentTemplates.REGION_PIVOT.formatted(region);
    }

    public String composeFomo() {
        int index = ThreadLocalRandom.current().nextInt(FOMO_MENTS.size());
        return FOMO_MENTS.get(index);
    }

    /**
     * discountPercent가 similarThresholdPercent 이상(유의미하게 저렴)이면 할인율을 보여주고,
     * 그 미만(비슷한 가격대)이면 %를 굳이 노출하지 않는 문구로 분기한다.
     */
    public String composePriceTarget(String region, double discountPercent, double similarThresholdPercent) {
        if (discountPercent < similarThresholdPercent) {
            return RecommendationMentTemplates.PRICE_TARGET_BUDGET_FIT;
        }

        long roundedDiscountPercent = Math.round(discountPercent);
        return isBlank(region)
                ? RecommendationMentTemplates.DEFAULT
                : RecommendationMentTemplates.PRICE_TARGET_DISCOUNT.formatted(region, roundedDiscountPercent);
    }

    public String composeRegionCheaperNearby(String region) {
        return isBlank(region)
                ? RecommendationMentTemplates.DEFAULT
                : RecommendationMentTemplates.REGION_CHEAPER_NEARBY.formatted(region);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
