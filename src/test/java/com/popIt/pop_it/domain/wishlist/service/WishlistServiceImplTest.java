package com.popIt.pop_it.domain.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.embedding.service.UserVectorService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private UserVectorService userVectorService;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Test
    void 찜_등록_성공하면_취향_벡터를_재계산한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findByIdAndDeletedAtIsNull(spaceId)).willReturn(Optional.of(Space.builder().id(spaceId).build()));
        given(wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)).willReturn(false);

        WishlistResDTO.WishlistToggleRes result = wishlistService.toggle(userId, spaceId);

        assertThat(result.isWishlisted()).isTrue();
        verify(userVectorService).recomputeUserVector(userId);
    }

    @Test
    void 찜_해제하면_취향_벡터를_재계산한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findByIdAndDeletedAtIsNull(spaceId)).willReturn(Optional.of(Space.builder().id(spaceId).build()));
        given(wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)).willReturn(true);

        WishlistResDTO.WishlistToggleRes result = wishlistService.toggle(userId, spaceId);

        assertThat(result.isWishlisted()).isFalse();
        verify(userVectorService).recomputeUserVector(userId);
    }

    @Test
    void 동시_요청으로_유니크_제약_충돌이_나면_재계산을_다시_하지_않는다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findByIdAndDeletedAtIsNull(spaceId)).willReturn(Optional.of(Space.builder().id(spaceId).build()));
        given(wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)).willReturn(false);
        given(wishlistRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicate"));

        wishlistService.toggle(userId, spaceId);

        // 이미 등록돼 있던 것으로 간주하는 경로라, 재계산은 그 등록을 성공시킨 다른 요청 쪽에서 이미 처리했을 것
        verify(userVectorService, never()).recomputeUserVector(any());
    }

    @Test
    void 취향_벡터_재계산이_실패해도_찜_처리_결과는_정상_반환된다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findByIdAndDeletedAtIsNull(spaceId)).willReturn(Optional.of(Space.builder().id(spaceId).build()));
        given(wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)).willReturn(false);
        willThrow(new IllegalStateException("redis down")).given(userVectorService).recomputeUserVector(userId);

        WishlistResDTO.WishlistToggleRes result = wishlistService.toggle(userId, spaceId);

        assertThat(result.isWishlisted()).isTrue();
    }
}
