package com.popIt.pop_it.domain.space.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.recommendation.RecommendationReasonEvaluator;
import com.popIt.pop_it.domain.space.recommendation.UserRecommendationContextResolver;
import com.popIt.pop_it.domain.space.repository.SpaceEmbeddingRepository;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.embedding.service.UserVectorService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpaceRecommendationServiceTest {

    @Mock
    private SpaceEmbeddingRepository spaceEmbeddingRepository;
    @Mock
    private SpaceImageRepository spaceImageRepository;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private UserActivityRepository userActivityRepository;
    @Mock
    private UserVectorService userVectorService;
    @Mock
    private UserRecommendationContextResolver userRecommendationContextResolver;
    @Mock
    private RecommendationReasonEvaluator recommendationReasonEvaluator;

    @InjectMocks
    private SpaceRecommendationService spaceRecommendationService;

    @Test
    void 취향_벡터가_없는_신규_유저는_빈_추천_목록을_받는다() {
        Long userId = 1L;
        given(userVectorService.getUserVector(userId)).willReturn(Optional.empty());

        SpaceResDTO.AiRecommendedSpaceListRes result =
                spaceRecommendationService.getRecommendedSpaces(userId, null, 10);

        assertThat(result.spaces()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        // 매칭되는 공간이 없어 빈 것과 구분할 수 있도록 이력 자체가 없다는 신호를 내려줘야 한다
        assertThat(result.hasActivityHistory()).isFalse();
        verify(spaceEmbeddingRepository, never()).findAllByEmbeddingIsNotNullAndDeletedAtIsNull();
    }

    @Test
    void 취향_벡터가_있으면_코사인_유사도_순으로_정렬한다() {
        Long userId = 1L;
        Space closestSpace = spaceWithEmbedding(1L, new float[]{1f, 0f}); // 유사도 1.0
        Space moderateSpace = spaceWithEmbedding(2L, new float[]{0.6f, 0.8f}); // 유사도 0.6 (기준 통과)
        Space farSpace = spaceWithEmbedding(3L, new float[]{0f, 1f}); // 유사도 0.0 (기준 미달로 제외)

        given(userVectorService.getUserVector(userId)).willReturn(Optional.of(new float[]{1f, 0f}));
        given(spaceEmbeddingRepository.findAllByEmbeddingIsNotNullAndDeletedAtIsNull())
                .willReturn(List.of(farSpace, moderateSpace, closestSpace));
        given(spaceImageRepository.findThumbnailsBySpaceIds(any())).willReturn(List.of());
        given(wishlistRepository.findByUserId(userId)).willReturn(List.of());

        SpaceResDTO.AiRecommendedSpaceListRes result =
                spaceRecommendationService.getRecommendedSpaces(userId, null, 10);

        assertThat(result.spaces()).extracting(SpaceResDTO.AiRecommendedSpaceRes::spaceId)
                .containsExactly(1L, 2L);
    }

    @Test
    void 최소_유사도_기준_미만인_공간만_있으면_추천_목록이_빈다() {
        Long userId = 1L;
        Space farSpace = spaceWithEmbedding(1L, new float[]{0f, 1f}); // 유사도 0.0, 기준(0.5) 미달

        given(userVectorService.getUserVector(userId)).willReturn(Optional.of(new float[]{1f, 0f}));
        given(spaceEmbeddingRepository.findAllByEmbeddingIsNotNullAndDeletedAtIsNull())
                .willReturn(List.of(farSpace));
        given(wishlistRepository.findByUserId(userId)).willReturn(List.of());

        SpaceResDTO.AiRecommendedSpaceListRes result =
                spaceRecommendationService.getRecommendedSpaces(userId, null, 10);

        assertThat(result.spaces()).isEmpty();
        // 이력은 있는데 기준을 넘는 공간이 없어서 빈 것이므로, 이력 자체가 없다는 신호와는 구분돼야 한다
        assertThat(result.hasActivityHistory()).isTrue();
    }

    @Test
    void 이미_찜한_공간은_추천_후보에서_제외된다() {
        Long userId = 1L;
        Space wishlistedSpace = spaceWithEmbedding(1L, new float[]{1f, 0f}); // 유사도 1.0으로 가장 가깝지만 이미 찜함
        Space otherSpace = spaceWithEmbedding(2L, new float[]{0.9f, 0.436f}); // 기준(0.5) 통과하는 차순위

        given(userVectorService.getUserVector(userId)).willReturn(Optional.of(new float[]{1f, 0f}));
        given(spaceEmbeddingRepository.findAllByEmbeddingIsNotNullAndDeletedAtIsNull())
                .willReturn(List.of(wishlistedSpace, otherSpace));
        given(wishlistRepository.findByUserId(userId)).willReturn(List.of(
                Wishlist.builder().userId(userId).spaceId(1L).build()
        ));
        given(spaceImageRepository.findThumbnailsBySpaceIds(any())).willReturn(List.of());

        SpaceResDTO.AiRecommendedSpaceListRes result =
                spaceRecommendationService.getRecommendedSpaces(userId, null, 10);

        assertThat(result.spaces()).extracting(SpaceResDTO.AiRecommendedSpaceRes::spaceId)
                .containsExactly(2L);
        assertThat(result.spaces()).allMatch(space -> !space.isWishlisted());
    }

    @Test
    void 이미_조회한_공간은_추천_후보에서_제외된다() {
        Long userId = 1L;
        Space viewedSpace = spaceWithEmbedding(1L, new float[]{1f, 0f}); // 유사도 1.0으로 가장 가깝지만 이미 조회함
        Space otherSpace = spaceWithEmbedding(2L, new float[]{0.9f, 0.436f}); // 기준(0.5) 통과하는 차순위

        given(userVectorService.getUserVector(userId)).willReturn(Optional.of(new float[]{1f, 0f}));
        given(spaceEmbeddingRepository.findAllByEmbeddingIsNotNullAndDeletedAtIsNull())
                .willReturn(List.of(viewedSpace, otherSpace));
        given(wishlistRepository.findByUserId(userId)).willReturn(List.of());
        given(userActivityRepository.findByUserId(userId)).willReturn(List.of(
                UserActivity.builder().userId(userId).spaceId(1L).build()
        ));
        given(spaceImageRepository.findThumbnailsBySpaceIds(any())).willReturn(List.of());

        SpaceResDTO.AiRecommendedSpaceListRes result =
                spaceRecommendationService.getRecommendedSpaces(userId, null, 10);

        assertThat(result.spaces()).extracting(SpaceResDTO.AiRecommendedSpaceRes::spaceId)
                .containsExactly(2L);
    }

    private Space spaceWithEmbedding(Long id, float[] embedding) {
        return Space.builder()
                .id(id)
                .buildingName("테스트 공간 " + id)
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .pricePerDay(10000)
                .embedding(embedding)
                .build();
    }
}
