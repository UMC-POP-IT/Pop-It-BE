package com.popIt.pop_it.domain.wishlist.dto;

public class WishlistResDTO {

    public record Toggle(
            Long spaceId,
            boolean isWishlisted
    ) {}
}
