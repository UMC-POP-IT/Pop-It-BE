package com.popIt.pop_it.domain.wishlist.converter;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

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

    // 찜한 공간 페이지 + 썸네일/찜수 맵을 조합해 목록 응답으로 변환
    public static WishlistResDTO.MyWishlistResult toMyWishlistResult(
            Page<Space> spacePage,
            Map<Long, String> thumbnailUrlBySpaceId,
            Map<Long, Integer> wishCountBySpaceId
    ) {
        List<WishlistResDTO.WishlistItem> items = spacePage.getContent().stream()
                .map(space -> new WishlistResDTO.WishlistItem(
                        space.getId(),
                        space.getBuildingName(),
                        space.getRoadAddress(),
                        space.getSpaceCategory().name(), // basicInfo = 카테고리 enum 이름
                        space.getPricePerDay(),
                        null, // pricePerWeek: 별도 컬럼 없음 → 프론트가 일 가격으로 환산
                        null, // pricePerMonth: 별도 컬럼 없음 → 프론트가 일 가격으로 환산
                        thumbnailUrlBySpaceId.get(space.getId()),
                        wishCountBySpaceId.getOrDefault(space.getId(), 0)
                ))
                .toList();

        return new WishlistResDTO.MyWishlistResult(items, spacePage.hasNext());
    }
}
