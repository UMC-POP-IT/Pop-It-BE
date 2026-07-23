package com.popIt.pop_it.domain.space.dto;

import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public class SpaceReqDTO {

        @Schema(description = "공간 등록 요청")
        public record Create(
                @Schema(description = "건물명", example = "합정 메세나폴리스")
                @NotBlank
                @Size(max = 20)
                String buildingName,

                @Schema(description = "등록자 유형 (현재 OWNER만 지원)", example = "OWNER")
                @NotNull
                RegistrantType registrantType,

                @Schema(description = "건물 유형", example = "LARGE_OFFICE")
                @NotNull
                BuildingType buildingType,

                @Schema(description = "시", example = "서울특별시")
                @NotBlank
                @Size(max = 50)
                String city,

                @Schema(description = "구", example = "마포구")
                @NotBlank
                @Size(max = 50)
                String district,

                @Schema(description = "도로명 주소 (다음 우편번호 위젯 결과)", example = "서울특별시 마포구 합정동 130-3")
                @NotBlank
                String roadAddress,

                @Schema(description = "상세 주소 (동/호수 등, 사용자 직접 입력)", example = "302동 302호")
                @NotBlank
                @Size(max = 30)
                String addressDetail,

                @Schema(description = "위도 (카카오 지도 SDK geocoder 변환값)", example = "37.5012")
                @NotNull
                @DecimalMin(value = "-90.0")
                @DecimalMax(value = "90.0")
                Double latitude,

                @Schema(description = "경도 (카카오 지도 SDK geocoder 변환값)", example = "127.0397")
                @NotNull
                @DecimalMin(value = "-180.0")
                @DecimalMax(value = "180.0")
                Double longitude,

                @Schema(description = "보증금", example = "4500000")
                @NotNull
                @PositiveOrZero
                Long deposit,

                @Schema(description = "일 단위 가격", example = "90000")
                @NotNull
                @Positive
                Integer pricePerDay,

                @Schema(description = "계약 가능 시작일 (yyyy-MM-dd)", example = "2026-06-01")
                @NotNull
                LocalDate availableStartDate,

                @Schema(description = "계약 가능 종료일 (yyyy-MM-dd). 시작일보다 빠를 수 없음", example = "2026-12-31")
                @NotNull
                LocalDate availableEndDate,

                @Schema(description = "공간 용도(카테고리)", example = "POPUP_STORE")
                @NotNull
                SpaceCategory spaceCategory,

                @Schema(description = "공간 구조 유형", example = "OPEN_HALL")
                @NotNull
                SpaceType spaceType,

                @Schema(description = "전용 면적", example = "66.0")
                @NotNull
                @Positive
                Double exclusiveArea,

                @Schema(description = "층 종류", example = "GENERAL_FLOOR")
                @NotNull
                FloorType floorType,

                @Schema(description = "층수. 지하·옥탑 등 층수 표기가 없으면 null", example = "2", nullable = true)
                Integer floorNumber,

                @Schema(description = "주차 가능 여부", example = "true")
                @NotNull
                Boolean parkingAvailable,

                @Schema(description = "공간 소개 (최대 1000자)", example = "홍대, 합정 중심지에 위치한 공간입니다.")
                @NotBlank
                @Size(max = 1000)
                String description,

                @Schema(
                        description = "시설 ID 목록 (GET /api/v1/facilities 응답의 facilityId). 선택하지 않으면 null 또는 빈 배열",
                        example = "[1, 3, 5]",
                        nullable = true
                )
                List<Long> facilityIds,

                @Schema(
                        description = "공간 사진 URL 목록.(최소 3장, 최대 10장) presigned URL 발급(POST /uploads/presigned-url) 후 S3 업로드하고 받은 fileUrl. "
                                + "배열 순서가 노출 순서이며 첫 번째가 대표 이미지입니다.",
                        example = "[\"https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid1.jpg\", "
                            + "\"https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid2.jpg\", "
                            + "\"https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid3.jpg\"]"
                )
                @NotEmpty
                @Size(min = 3, max = 10)
                List<@NotBlank String> imageUrls
        ) {}

        @Schema(description = "공간 탐색 요청 (쿼리 파라미터)")
        public record Search(
                @Schema(
                        description = "통합 검색어. 공간명 / 지역 이름(구·동·도로명 주소) / 정보(공간 용도·구조 유형)를 부분 일치로 검색합니다. "
                                + "예: \"성수\", \"강남구\", \"팝업스토어\", \"오픈형 홀\"",
                        example = "성수"
                )
                @Size(max = 50)
                String keyword,

                @Schema(
                        description = "공간 용도(카테고리) 필터. 생략하면 '전체'",
                        example = "POPUP_STORE"
                )
                SpaceCategory spaceCategory,

                @Schema(
                        description = "지역(구) 필터. 서울 25개 구 중 하나. 생략하면 전체 지역",
                        example = "마포구"
                )
                @Size(max = 50)
                String district,

                @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
                @Min(0)
                Integer page,

                @Schema(description = "페이지 크기 (1~50). 기본값은 그리드 레이아웃 4x7 기준", example = "28", defaultValue = "28")
                @Min(1) @Max(50)
                Integer size
        ) {
                public Search {
                        if (page == null) {
                                page = 0;
                        }

                        if (size == null) {
                                size = 28;
                        }
                }
        }
}