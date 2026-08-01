package com.popIt.pop_it.domain.space.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

public class AiRecommendationResDTO {

    private AiRecommendationResDTO() {
    }

    @Schema(description = "AI 맞춤 추천 공간 목록 항목")
    @Builder
    public record AiRecommendedSpaceRes(
            @Schema(description = "공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "합정 메세나폴리스")
            String buildingName,

            @Schema(description = "추천 사유 태그 멘트. 유저 이력/공간 상태에 따라 동적으로 결정되며, 해당하는 사유가 없으면 기본 문구가 내려간다", example = "다른 사장님들 문의 급증")
            String tag,

            @Schema(description = "지역(구)", example = "마포구")
            String district,

            @Schema(description = "도로명 주소", example = "서울특별시 마포구 합정동 130-3")
            String roadAddress,

            @Schema(description = "전용 면적", example = "66.0")
            Double exclusiveArea,

            @Schema(description = "공간 용도(카테고리). 카드 이미지 우하단 배지에 사용", example = "POPUP_STORE")
            String spaceCategory,

            @Schema(
                    description = "카드 하단에 노출할 키워드. (#지역명(동), #공간유형) 순서. "
                            + "동 정보가 없는 공간은 공간유형 하나만 내려간다.",
                    example = "[\"#합정동\", \"#팝업스토어\"]"
            )
            List<String> keywords,

            @Schema(description = "일 단위 가격", example = "90000")
            Integer pricePerDay,

            @Schema(description = "주 단위 가격 (일 단위 가격 * 7로 산출)", example = "630000")
            Integer pricePerWeek,

            @Schema(description = "월 단위 가격 (일 단위 가격 * 30로 산출)", example = "2700000")
            Integer pricePerMonth,

            @Schema(description = "대표 사진 URL (사진 목록의 첫 번째). 사진이 없으면 null", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid1.jpg")
            String thumbnailUrl,

            @Schema(description = "주차 가능 여부", example = "true")
            Boolean parkingAvailable,

            @Schema(description = "요청자의 찜 여부. 이미 찜한 공간은 추천 후보 단계에서 제외되므로 항상 false", example = "false")
            Boolean isWishlisted,

            @Schema(description = "해당 공간의 총 찜 수", example = "20")
            Integer wishCount
    ) {}

    @Schema(description = "AI 맞춤 추천 공간 목록 조회 응답")
    @Builder
    public record AiRecommendedSpaceListRes(
            @Schema(description = "추천 공간 목록")
            List<AiRecommendedSpaceRes> spaces,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            Boolean hasNext,

            @Schema(description = "다음 페이지 조회용 커서. hasNext가 false면 null", example = "10", nullable = true)
            String nextCursor,

            @Schema(description = "찜/조회 이력이 있어 맞춤 추천을 시도했는지 여부. false면 이력이 없어 spaces가 항상 빈 배열입니다.", example = "true")
            Boolean hasActivityHistory
    ) {}
}
