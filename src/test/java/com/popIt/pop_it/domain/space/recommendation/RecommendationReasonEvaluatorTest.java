package com.popIt.pop_it.domain.space.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceDailyUv;
import com.popIt.pop_it.domain.space.recommendation.RegionCenterResolver.RegionCenter;
import com.popIt.pop_it.domain.space.repository.SpaceDailyUvRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationReasonEvaluatorTest {

    @Mock
    private SpaceDailyUvRepository spaceDailyUvRepository;
    @Mock
    private RegionCenterResolver regionCenterResolver;

    private RecommendationReasonEvaluator evaluator;

    private static final RegionCenter GANGNAM_CENTER = new RegionCenter(37.4979, 127.0276);
    private static final RegionCenter HAPJEONG_CENTER = new RegionCenter(37.55, 126.92);
    private static final Clock CLOCK = Clock.system(ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        evaluator = new RecommendationReasonEvaluator(
                spaceDailyUvRepository, regionCenterResolver,
                new RecommendationMentComposer(), CLOCK);
    }

    private Space space(Long id, String dong, double lat, double lng, int price) {
        return Space.builder()
                .id(id).dong(dong).latitude(lat).longitude(lng)
                .pricePerDay(price)
                .build();
    }

    // uv24h는 "어제"(스케줄러가 매일 00:10에 확정 저장하는 날짜) 행으로, baselineDailyUv는
    // 그 전 날짜부터 순서대로 "직전 7일 베이스라인" 행으로 SpaceDailyUv 목록을 만든다
    private List<SpaceDailyUv> dailyUvHistory(Long spaceId, int uv24h, int... baselineDailyUv) {
        LocalDate yesterday = LocalDate.now(CLOCK).minusDays(1);
        List<SpaceDailyUv> history = new ArrayList<>();
        history.add(SpaceDailyUv.builder().spaceId(spaceId).visitDate(yesterday).uvCount(uv24h).build());
        for (int i = 0; i < baselineDailyUv.length; i++) {
            history.add(SpaceDailyUv.builder()
                    .spaceId(spaceId).visitDate(yesterday.minusDays(i + 1)).uvCount(baselineDailyUv[i]).build());
        }
        return history;
    }

    @Test
    void 콜드_타겟_후보_공간의_동이_유저_이력에_없으면_추천된다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        given(regionCenterResolver.resolveCenter("합정")).willReturn(Optional.of(HAPJEONG_CENTER));
        // 유저는 "합정"만 다녀봤고 "홍대"는 아직 안 가봄
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("합정"), "합정", 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(RecommendationReasonType.COLD_TARGET_NEARBY);
        assertThat(result.get().mentText()).isEqualTo("합정과 가까운 추천 공간");
    }

    @Test
    void 콜드_타겟_후보_공간의_동을_이미_가본_적_있으면_추천되지_않는다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대", "합정"), "합정", 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void 콜드_타겟_활동_이력이_전혀_없으면_추천되지_않는다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        // 이력이 없으니 "홍대"가 visitedDongs에 없는 건 당연하지만, 개인화 신호 자체가 없으므로 추천 대상이 아니어야 함
        UserRecommendationContext context = new UserRecommendationContext(
                1L, false, Set.of(), null, 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void 지역_피벗_반복_조회_지역_반경_내_공간이면_추천된다() {
        Space candidate = space(1L, "서초구", 37.49, 127.02, 60000);
        given(regionCenterResolver.resolveCenter("강남구")).willReturn(Optional.of(GANGNAM_CENTER));
        // 후보 자신의 동(서초구)은 이미 가본 적 있는 것으로 설정해 COLD_TARGET_NEARBY와 겹치지 않게 함
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("서초구"), "강남구", 5, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(RecommendationReasonType.REGION_PIVOT);
        assertThat(result.get().mentText()).isEqualTo("강남구 대신 여기 어때요?");
    }

    @Test
    void 지역_피벗_조회_횟수가_기준_미만이고_찜도_없으면_추천되지_않는다() {
        Space candidate = space(1L, "서초구", 37.49, 127.02, 60000);
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("서초구"), "강남구", 2, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void 지역_피벗_조회_횟수가_기준_미만이어도_찜을_1회_이상_했으면_추천된다() {
        Space candidate = space(1L, "서초구", 37.49, 127.02, 60000);
        given(regionCenterResolver.resolveCenter("강남구")).willReturn(Optional.of(GANGNAM_CENTER));
        // 조회는 0회지만 찜을 1회 했으므로 "찜했거나 3회 이상 조회" 조건을 만족해야 한다
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("서초구"), "강남구", 0, 1, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(RecommendationReasonType.REGION_PIVOT);
    }

    @Test
    void FOMO_최근_UV가_급증하면_추천된다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        // uv24h=20, 직전 7일 평균=10 -> 200% 급증
        given(spaceDailyUvRepository.findTop8BySpaceIdOrderByVisitDateDesc(1L))
                .willReturn(dailyUvHistory(1L, 20, 10, 10, 10, 10, 10, 10, 10));
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), null, 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(RecommendationReasonType.FOMO);
    }

    @Test
    void FOMO_UV는_많아도_급증_비율이_기준_미만이면_추천되지_않는다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        // uv24h=16, 직전 7일 평균=15 -> 약 107%로 150% 미만
        given(spaceDailyUvRepository.findTop8BySpaceIdOrderByVisitDateDesc(1L))
                .willReturn(dailyUvHistory(1L, 16, 15, 15, 15, 15, 15, 15, 15));
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), null, 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void FOMO_비교할_베이스라인_이력이_없으면_추천되지_않는다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        given(spaceDailyUvRepository.findTop8BySpaceIdOrderByVisitDateDesc(1L))
                .willReturn(dailyUvHistory(1L, 100)); // 어제치밖에 없음 -> 베이스라인 7일이 전부 0으로 채워져 평균이 0
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), null, 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void FOMO_어제치_집계가_없으면_더_오래된_기록이_있어도_추천되지_않는다() {
        // 방문이 없던 날은 SpaceDailyUv 행 자체가 생기지 않으므로, "가장 최근 행"이 실제로는
        // 어제가 아니라 며칠 전 데이터일 수 있다 - 이 경우 그 오래된 행을 uv24h로 착각하면 안 된다.
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        LocalDate threeDaysAgo = LocalDate.now(CLOCK).minusDays(3);
        given(spaceDailyUvRepository.findTop8BySpaceIdOrderByVisitDateDesc(1L))
                .willReturn(List.of(SpaceDailyUv.builder().spaceId(1L).visitDate(threeDaysAgo).uvCount(999).build()));
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), null, 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void FOMO_유저_활동_이력이_전혀_없으면_UV가_급증해도_추천되지_않는다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        UserRecommendationContext context = new UserRecommendationContext(
                1L, false, Set.of(), null, 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void 가격_타겟_평균가보다_유의미하게_저렴하면_할인율과_함께_추천된다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 40000);
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), "홍대", 0, 0, 50000.0, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(RecommendationReasonType.PRICE_TARGET);
        assertThat(result.get().mentText()).isEqualTo("최근 본 홍대 공간보다 20% 저렴해요");
        assertThat(result.get().calculatedValue()).isEqualTo(20.0);
    }

    @Test
    void 가격_타겟_평균가와_비슷하면_할인율_없이_예산_문구로_추천된다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 49000);
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), "홍대", 0, 0, 50000.0, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().mentText()).isEqualTo("찾고 계신 예산대에 딱 맞는 공간!");
        assertThat(result.get().calculatedValue()).isNull();
    }

    @Test
    void 가격_타겟_평균가보다_많이_비싸면_추천되지_않는다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 80000);
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), "홍대", 0, 0, 50000.0, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }

    @Test
    void 가성비_인접_대표지역_평균가보다_충분히_저렴하고_반경_내면_추천된다() {
        Space candidate = space(1L, "서초구", 37.49, 127.02, 40000);
        given(regionCenterResolver.resolveCenter("강남구")).willReturn(Optional.of(GANGNAM_CENTER));
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("서초구"), "강남구", 0, 0, null, 50000.0);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(RecommendationReasonType.REGION_CHEAPER_NEARBY);
        assertThat(result.get().mentText()).isEqualTo("강남구 근처 가성비 공간");
    }

    @Test
    void 여러_조건이_동시에_만족되면_우선순위가_높은_태그가_선택된다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        given(regionCenterResolver.resolveCenter("합정")).willReturn(Optional.of(HAPJEONG_CENTER));
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("합정"), "합정", 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(RecommendationReasonType.COLD_TARGET_NEARBY);
        verifyNoInteractions(spaceDailyUvRepository);
    }

    @Test
    void 아무_조건도_만족하지_않으면_빈_값을_반환한다() {
        Space candidate = space(1L, "홍대", 37.55, 126.92, 50000);
        // 활동 이력은 있지만(hasRecentActivity=true) 후보의 동은 이미 가본 적 있고, 대표 지역도 없어 나머지 태그도 다 불충족
        UserRecommendationContext context = new UserRecommendationContext(
                1L, true, Set.of("홍대"), null, 0, 0, null, null);

        Optional<RecommendationReasonResult> result = evaluator.evaluate(candidate, context);

        assertThat(result).isEmpty();
    }
}
