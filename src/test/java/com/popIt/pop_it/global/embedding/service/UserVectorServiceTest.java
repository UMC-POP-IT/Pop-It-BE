package com.popIt.pop_it.global.embedding.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.embedding.redis.UserVectorRedisStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
