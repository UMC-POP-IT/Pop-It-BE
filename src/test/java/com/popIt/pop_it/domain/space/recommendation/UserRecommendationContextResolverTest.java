package com.popIt.pop_it.domain.space.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import com.popIt.pop_it.domain.user_event.repository.UserEventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRecommendationContextResolverTest {

    @Mock
    private UserEventRepository userEventRepository;
    @Mock
    private SpaceRepository spaceRepository;

    private UserRecommendationContextResolver resolver;

    private static final Clock CLOCK = Clock.system(ZoneId.of("Asia/Seoul"));

    private UserEvent event(Long spaceId, UserEventType type, String region) {
        return UserEvent.builder()
                .userId(1L).spaceId(spaceId).eventType(type).region(region)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 최근_이벤트가_없으면_대표_지역과_평균가_모두_null이다() {
        resolver = new UserRecommendationContextResolver(userEventRepository, spaceRepository, CLOCK);
        given(userEventRepository.findByUserIdAndCreatedAtAfter(eq(1L), any())).willReturn(List.of());

        UserRecommendationContext context = resolver.resolve(1L);

        assertThat(context.hasRecentActivity()).isFalse();
        assertThat(context.visitedDongs()).isEmpty();
        assertThat(context.representativeRegion()).isNull();
        assertThat(context.representativeRegionViewCount()).isZero();
        assertThat(context.representativeRegionWishlistCount()).isZero();
        assertThat(context.representativeRegionInteractionAvgPrice()).isNull();
        assertThat(context.representativeRegionAvgPrice()).isNull();
    }

    @Test
    void 가장_많이_조회_찜한_지역이_대표_지역이_되고_평균가도_함께_계산된다() {
        resolver = new UserRecommendationContextResolver(userEventRepository, spaceRepository, CLOCK);

        given(userEventRepository.findByUserIdAndCreatedAtAfter(eq(1L), any())).willReturn(List.of(
                event(10L, UserEventType.VIEW, "강남구"),
                event(11L, UserEventType.VIEW, "강남구"),
                event(12L, UserEventType.VIEW, "홍대"),
                event(13L, UserEventType.WISHLIST, "강남구")
        ));
        // representativeRegionInteractionAvgPrice는 대표 지역(강남구)에 속한 공간(10,11,13)만으로 계산되어야 하므로,
        // 대표 지역이 아닌 홍대 공간(12)은 findAllById 호출 대상에서 제외된다
        given(spaceRepository.findAllById(eq(Set.of(10L, 11L, 13L)))).willReturn(List.of(
                Space.builder().id(10L).pricePerDay(40000).build(),
                Space.builder().id(11L).pricePerDay(60000).build(),
                Space.builder().id(13L).pricePerDay(70000).build()
        ));
        given(spaceRepository.findAvgPricePerDayByDong("강남구")).willReturn(55000.0);

        UserRecommendationContext context = resolver.resolve(1L);

        assertThat(context.hasRecentActivity()).isTrue();
        assertThat(context.visitedDongs()).containsExactlyInAnyOrder("강남구", "홍대");
        // 강남구: VIEW 2회 + WISHLIST 1회 = 3회로 최다 (대표 지역 선정은 합산 빈도 기준)
        assertThat(context.representativeRegion()).isEqualTo("강남구");
        // 조회 수/찜 수는 REGION_PIVOT의 OR 판단을 위해 따로 유지된다
        assertThat(context.representativeRegionViewCount()).isEqualTo(2);
        assertThat(context.representativeRegionWishlistCount()).isEqualTo(1);
        assertThat(context.representativeRegionInteractionAvgPrice()).isEqualTo(170000.0 / 3); // (40000+60000+70000)/3, 홍대(12) 제외
        assertThat(context.representativeRegionAvgPrice()).isEqualTo(55000.0);
    }

    @Test
    void 조회_없이_찜만_있어도_그_지역이_대표_지역이_될_수_있다() {
        resolver = new UserRecommendationContextResolver(userEventRepository, spaceRepository, CLOCK);

        given(userEventRepository.findByUserIdAndCreatedAtAfter(eq(1L), any())).willReturn(List.of(
                event(20L, UserEventType.WISHLIST, "성수동"),
                event(21L, UserEventType.WISHLIST, "성수동"),
                event(22L, UserEventType.VIEW, "홍대")
        ));
        given(spaceRepository.findAllById(any())).willReturn(List.of(
                Space.builder().id(20L).pricePerDay(30000).build(),
                Space.builder().id(21L).pricePerDay(50000).build(),
                Space.builder().id(22L).pricePerDay(40000).build()
        ));
        given(spaceRepository.findAvgPricePerDayByDong("성수동")).willReturn(40000.0);

        UserRecommendationContext context = resolver.resolve(1L);

        assertThat(context.representativeRegion()).isEqualTo("성수동"); // 찜 2회로 조회(홍대 1회)보다 많음
        assertThat(context.representativeRegionViewCount()).isZero();
        assertThat(context.representativeRegionWishlistCount()).isEqualTo(2);
    }
}
