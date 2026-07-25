package com.popIt.pop_it.domain.wishlist.converter;

import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;

public class WishlistConverter {

    // 찜 등록 시 신규 엔티티 생성 (createdAt은 @CreationTimestamp로 자동 세팅)
    public static Wishlist toEntity(Long userId, Long spaceId) {
        return Wishlist.builder()
                .userId(userId)
                .spaceId(spaceId)
                .build();
    }

    public static WishlistResDTO.Toggle toToggle(Long spaceId, boolean isWishlisted) {
        return new WishlistResDTO.Toggle(spaceId, isWishlisted);
    }
}
