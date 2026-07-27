package com.popIt.pop_it.global.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.embedding.redis.UserVectorRedisStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserVectorServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private UserActivityRepository userActivityRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private UserVectorRedisStore userVectorRedisStore;

    @InjectMocks
    private UserVectorService userVectorService;

    @Test
    void 기간_제한_없이_오래된_찜_이력만_있어도_취향_벡터를_계산해_저장한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        Wishlist oldWishlist = Wishlist.builder()
                .userId(userId)
                .spaceId(spaceId)
                .createdAt(LocalDateTime.now().minusDays(30)) // 14일보다 훨씬 오래된 이력
                .build();
        Space space = Space.builder()
                .id(spaceId)
                .embedding(new float[]{1f, 0f, 0f})
                .build();

        given(wishlistRepository.findByUserId(userId)).willReturn(List.of(oldWishlist));
        given(userActivityRepository.findByUserId(userId)).willReturn(List.of());
        given(spaceRepository.findAllById(any())).willReturn(List.of(space));

        userVectorService.recomputeUserVector(userId);

        verify(userVectorRedisStore).save(anyLong(), any(float[].class));
    }

    @Test
    void 찜_조회_이력이_전혀_없는_신규_유저는_취향_벡터를_저장하지_않고_기존_캐시도_지운다() {
        Long userId = 2L;
        given(wishlistRepository.findByUserId(userId)).willReturn(List.of());
        given(userActivityRepository.findByUserId(userId)).willReturn(List.of());

        userVectorService.recomputeUserVector(userId);

        verify(userVectorRedisStore, never()).save(anyLong(), any(float[].class));
        // DB를 리셋해도 Redis에 예전 벡터가 남아있으면 userId 재사용 시 다른 유저가 이어받는 문제가 생기므로,
        // 이력이 없어진 시점에 명시적으로 지워야 한다
        verify(userVectorRedisStore).delete(userId);
    }

    @Test
    void 남아있는_이력의_공간에_임베딩이_없으면_벡터를_저장하지_않고_기존_캐시도_지운다() {
        Long userId = 3L;
        Wishlist wishlist = Wishlist.builder().userId(userId).spaceId(10L)
                .createdAt(LocalDateTime.now()).build();
        Space spaceWithoutEmbedding = Space.builder().id(10L).build(); // embedding == null

        given(wishlistRepository.findByUserId(userId)).willReturn(List.of(wishlist));
        given(userActivityRepository.findByUserId(userId)).willReturn(List.of());
        given(spaceRepository.findAllById(any())).willReturn(List.of(spaceWithoutEmbedding));

        userVectorService.recomputeUserVector(userId);

        verify(userVectorRedisStore, never()).save(anyLong(), any(float[].class));
        verify(userVectorRedisStore).delete(userId);
    }

    @Test
    void 조회_횟수가_많을수록_해당_공간_방향으로_취향_벡터가_더_강하게_반영된다() {
        Long userId = 4L;
        LocalDateTime now = LocalDateTime.now();
        Space rarelyViewedSpace = Space.builder().id(10L).embedding(new float[]{1f, 0f}).build();
        Space frequentlyViewedSpace = Space.builder().id(20L).embedding(new float[]{0f, 1f}).build();

        // 감쇠(같은 시각)와 액션 가중치(둘 다 VIEW)는 동일하게 맞춰서, viewCount 차이만으로
        // 결과 벡터가 갈리는지 격리해서 검증한다
        UserActivity rarelyViewed = UserActivity.builder()
                .userId(userId).spaceId(10L).viewCount(1).lastViewedAt(now).build();
        UserActivity frequentlyViewed = UserActivity.builder()
                .userId(userId).spaceId(20L).viewCount(9).lastViewedAt(now).build();

        given(wishlistRepository.findByUserId(userId)).willReturn(List.of());
        given(userActivityRepository.findByUserId(userId))
                .willReturn(List.of(rarelyViewed, frequentlyViewed));
        given(spaceRepository.findAllById(any()))
                .willReturn(List.of(rarelyViewedSpace, frequentlyViewedSpace));

        userVectorService.recomputeUserVector(userId);

        ArgumentCaptor<float[]> captor = ArgumentCaptor.forClass(float[].class);
        verify(userVectorRedisStore).save(eq(userId), captor.capture());

        // 가중치 비율(1:9)대로 [1,0]과 [0,1]을 섞으면 [0.1, 0.9]가 나와야 한다
        float[] result = captor.getValue();
        assertThat(result[0]).isCloseTo(0.1f, within(0.01f));
        assertThat(result[1]).isCloseTo(0.9f, within(0.01f));
    }

    @Test
    void 캐시가_있으면_DB를_조회하지_않고_캐시값을_그대로_반환한다() {
        Long userId = 5L;
        float[] cachedVector = new float[]{0.5f, 0.5f};
        given(userVectorRedisStore.find(userId)).willReturn(Optional.of(cachedVector));

        Optional<float[]> result = userVectorService.getUserVector(userId);

        assertThat(result).contains(cachedVector);
        verify(wishlistRepository, never()).findByUserId(any());
        verify(userActivityRepository, never()).findByUserId(any());
    }

    @Test
    void 캐시가_만료됐어도_이력이_남아있으면_새로_계산해서_반환하고_다시_캐싱한다() {
        Long userId = 6L;
        Long spaceId = 10L;
        Wishlist wishlist = Wishlist.builder().userId(userId).spaceId(spaceId)
                .createdAt(LocalDateTime.now()).build();
        Space space = Space.builder().id(spaceId).embedding(new float[]{1f, 0f}).build();

        // TTL(30일) 만료 등으로 캐시는 비어 있지만(첫 조회 empty), 재계산 이후에는 새로 저장된 값이
        // 있다고 가정한다(두 번째 조회에서 값 반환) - "이력은 있는데 캐시만 없는" 상황을 재현한다
        given(userVectorRedisStore.find(userId))
                .willReturn(Optional.empty(), Optional.of(new float[]{1f, 0f}));
        given(wishlistRepository.findByUserId(userId)).willReturn(List.of(wishlist));
        given(userActivityRepository.findByUserId(userId)).willReturn(List.of());
        given(spaceRepository.findAllById(any())).willReturn(List.of(space));

        Optional<float[]> result = userVectorService.getUserVector(userId);

        // 이력이 없는 신규 유저로 오판하지 않고, DB 이력으로 재계산해 캐시를 다시 채웠는지 확인
        assertThat(result).isPresent();
        verify(userVectorRedisStore).save(eq(userId), any(float[].class));
    }

    @Test
    void 캐시도_없고_이력도_전혀_없으면_빈_값을_반환한다() {
        Long userId = 7L;
        given(userVectorRedisStore.find(userId)).willReturn(Optional.empty());
        given(wishlistRepository.findByUserId(userId)).willReturn(List.of());
        given(userActivityRepository.findByUserId(userId)).willReturn(List.of());

        Optional<float[]> result = userVectorService.getUserVector(userId);

        assertThat(result).isEmpty();
        verify(userVectorRedisStore, never()).save(anyLong(), any(float[].class));
    }
}
