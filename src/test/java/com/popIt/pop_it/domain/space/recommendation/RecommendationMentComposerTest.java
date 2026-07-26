package com.popIt.pop_it.domain.space.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecommendationMentComposerTest {

    private final RecommendationMentComposer composer = new RecommendationMentComposer();

    @Test
    void 콜드_타겟_멘트는_지역명을_포함한다() {
        assertThat(composer.composeColdTargetNearby("마포구")).isEqualTo("마포구과 가까운 추천 공간");
    }

    @Test
    void 콜드_타겟_지역명이_없으면_기본_문구로_대체한다() {
        assertThat(composer.composeColdTargetNearby(null)).isEqualTo("관심 지역과 가까운 추천 공간");
        assertThat(composer.composeColdTargetNearby(" ")).isEqualTo("관심 지역과 가까운 추천 공간");
    }

    @Test
    void 지역_피벗_멘트는_지역명을_포함한다() {
        assertThat(composer.composeRegionPivot("강남구")).isEqualTo("강남구 대신 여기 어때요?");
    }

    @Test
    void 지역_피벗_지역명이_없으면_기본_문구로_대체한다() {
        assertThat(composer.composeRegionPivot(null)).isEqualTo("관심 있는 공간과 비슷해요");
    }

    @Test
    void FOMO_멘트는_두_후보_중_하나다() {
        String ment = composer.composeFomo();

        assertThat(ment).isIn("다른 사장님들 문의 급증", "최근 가장 핫한 공간이에요");
    }

    @Test
    void 가격_타겟_할인율이_기준_이상이면_할인율을_보여준다() {
        String ment = composer.composePriceTarget("성수동", 20.4, 5.0);

        assertThat(ment).isEqualTo("최근 본 성수동 공간보다 20% 저렴해요");
    }

    @Test
    void 가격_타겟_할인율은_있는데_지역명이_없으면_기본_문구로_대체한다() {
        String ment = composer.composePriceTarget(null, 20.0, 5.0);

        assertThat(ment).isEqualTo("관심 있는 공간과 비슷해요");
    }

    @Test
    void 가격_타겟_할인율이_기준_미만이면_예산_문구를_보여준다() {
        String ment = composer.composePriceTarget("성수동", 3.0, 5.0);

        assertThat(ment).isEqualTo("찾고 계신 예산대에 딱 맞는 공간!");
    }

    @Test
    void 가성비_인접_멘트는_지역명을_포함한다() {
        assertThat(composer.composeRegionCheaperNearby("홍대")).isEqualTo("홍대 근처 가성비 공간");
    }

    @Test
    void 가성비_인접_지역명이_없으면_기본_문구로_대체한다() {
        assertThat(composer.composeRegionCheaperNearby(null)).isEqualTo("관심 있는 공간과 비슷해요");
    }
}
