package com.popIt.pop_it.domain.wishlist.service;

import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;

public interface WishlistService {

    // 찜 상태를 토글한다: 이미 찜한 공간이면 해제, 아니면 등록
    WishlistResDTO.Toggle toggle(Long userId, Long spaceId);
}
