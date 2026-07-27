package com.popIt.pop_it.domain.wishlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.embedding.event.UserEngagementEvent;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Test
    void 찜_등록_성공하면_취향_벡터_재계산_이벤트를_발행한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findByIdAndDeletedAtIsNull(spaceId)).willReturn(Optional.of(Space.builder().id(spaceId).build()));
        given(wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)).willReturn(false);

        WishlistResDTO.WishlistToggleRes result = wishlistService.toggle(userId, spaceId);

        assertThat(result.isWishlisted()).isTrue();
        verify(eventPublisher).publishEvent(eq(new UserEngagementEvent(userId)));
    }

    @Test
    void 찜_해제하면_취향_벡터_재계산_이벤트를_발행한다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findByIdAndDeletedAtIsNull(spaceId)).willReturn(Optional.of(Space.builder().id(spaceId).build()));
        given(wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)).willReturn(true);

        WishlistResDTO.WishlistToggleRes result = wishlistService.toggle(userId, spaceId);

        assertThat(result.isWishlisted()).isFalse();
        verify(eventPublisher).publishEvent(eq(new UserEngagementEvent(userId)));
    }

    @Test
    void 동시_요청으로_유니크_제약_충돌이_나면_재계산_이벤트를_다시_발행하지_않는다() {
        Long userId = 1L;
        Long spaceId = 10L;
        given(spaceRepository.findByIdAndDeletedAtIsNull(spaceId)).willReturn(Optional.of(Space.builder().id(spaceId).build()));
        given(wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)).willReturn(false);
        given(wishlistRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicate"));

        wishlistService.toggle(userId, spaceId);

        // 이미 등록돼 있던 것으로 간주하는 경로라, 재계산은 그 등록을 성공시킨 다른 요청 쪽에서 이미 처리했을 것
        verify(eventPublisher, never()).publishEvent(any());
    }
}
