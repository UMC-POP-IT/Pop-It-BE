package com.popIt.pop_it.domain.wishlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class WishlistResDTO {

    public record WishlistToggleRes(
            Long spaceId,
            boolean isWishlisted
    ) {}

    @Schema(description = "내가 찜한 공간 목록 항목")
    public record WishlistItemRes(
            @Schema(description = "공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "강남 OOO 건물")
            String buildingName,

            @Schema(description = "도로명 주소", example = "서울특별시 강남구 역삼동 130-3")
            String roadAddress,

            @Schema(description = "공간 용도(카테고리 enum 이름)", example = "POPUP_STORE")
            String basicInfo,

            @Schema(description = "일 단위 가격(원)", example = "70000")
            Integer pricePerDay,

            @Schema(description = "주 단위 가격(원). 별도 컬럼이 없어 항상 null (프론트에서 일 가격으로 환산)", nullable = true)
            Integer pricePerWeek,

            @Schema(description = "월 단위 가격(원). 별도 컬럼이 없어 항상 null (프론트에서 일 가격으로 환산)", nullable = true)
            Integer pricePerMonth,

            @Schema(description = "대표 사진 URL. 사진이 없으면 null", nullable = true, example = "https://s3.amazonaws.com/popIt/img1.jpg")
            String thumbnailUrl,

            @Schema(description = "해당 공간의 총 찜 수", example = "20")
            Integer wishCount
    ) {}

    @Schema(description = "내가 찜한 공간 목록 조회 응답")
    public record WishlistListRes(
            @Schema(description = "찜한 공간 목록 (최근 찜한 순)")
            List<WishlistItemRes> wishlist,

            @Schema(description = "다음 페이지 존재 여부", example = "false")
            boolean hasNext
    ) {}
}
