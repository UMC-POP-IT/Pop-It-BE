package com.popIt.pop_it.domain.space.dto;

import com.popIt.pop_it.domain.facility.enums.FacilityCategory;
import com.popIt.pop_it.domain.space.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class SpaceResDTO {

    // 공간 등록
    @Schema(description = "공간 등록 응답")
    @Builder
    public record CreateResult(
            @Schema(description = "등록된 공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "합정 메세나폴리스")
            String buildingName
    ) {}

    // 공간 상세 조회
    @Schema(description = "공간에 연결된 시설 항목")
    public record FacilityItem(
            @Schema(description = "시설 ID (공간 수정 화면의 체크박스 초기값 매핑용)", example = "1")
            Long facilityId,

            @Schema(description = "시설 카테고리", example = "HEATING_COOLING")
            FacilityCategory category,

            @Schema(description = "시설명", example = "개별 난방")
            String name
    ) {}

    @Schema(description = "공간 상세 조회 응답")
    @Builder
    public record Detail(
            @Schema(description = "공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "합정 메세나폴리스")
            String buildingName,

            @Schema(description = "등록자 유형", example = "OWNER")
            RegistrantType registrantType,

            @Schema(description = "건물 유형", example = "LARGE_OFFICE")
            BuildingType buildingType,

            @Schema(description = "시", example = "서울특별시")
            String city,

            @Schema(description = "구", example = "마포구")
            String district,

            @Schema(description = "도로명 주소", example = "서울특별시 마포구 합정동 130-3")
            String roadAddress,

            @Schema(description = "상세 주소", example = "302동 302호")
            String addressDetail,

            @Schema(description = "위도", example = "37.5012")
            Double latitude,

            @Schema(description = "경도", example = "127.0397")
            Double longitude,

            @Schema(description = "보증금", example = "4500000")
            Long deposit,

            @Schema(description = "일 단위 가격", example = "90000")
            Integer pricePerDay,

            @Schema(description = "계약 가능 시작일", example = "2026-06-01")
            LocalDate availableStartDate,

            @Schema(description = "계약 가능 종료일", example = "2026-12-31")
            LocalDate availableEndDate,

            @Schema(description = "공간 용도(카테고리)", example = "POPUP_STORE")
            SpaceCategory spaceCategory,

            @Schema(description = "공간 구조 유형", example = "OPEN_HALL")
            SpaceType spaceType,

            @Schema(description = "전용 면적", example = "66.0")
            Double exclusiveArea,

            @Schema(description = "층 종류", example = "GENERAL_FLOOR")
            FloorType floorType,

            @Schema(description = "층수. 지하·옥탑 등 층수 표기가 없으면 null", example = "2", nullable = true)
            Integer floorNumber,

            @Schema(description = "주차 가능 여부", example = "true")
            Boolean parkingAvailable,

            @Schema(description = "연결된 시설 목록. 없으면 빈 배열")
            List<FacilityItem> facilities,

            @Schema(description = "공간 소개", example = "홍대, 합정 중심지에 위치한 공간입니다.")
            String description,

            @Schema(description = "공간 사진 URL 목록. 등록 시 저장한 노출 순서대로 내려가며 첫 번째가 대표 이미지")
            List<String> imageUrls,

            @Schema(description = "요청자가 이 공간의 호스트인지 여부. 비로그인 시 항상 false", example = "false")
            Boolean isMine,

            @Schema(description = "요청자의 찜 여부. 비로그인 시 항상 false", example = "false")
            Boolean isWishlisted,

            @Schema(description = "해당 공간의 총 찜 수 (로그인 여부와 무관)", example = "20")
            Integer wishCount
    ) {}

    @Schema(description = "내 공간 목록 항목")
    @Builder
    public record MySpace(
            @Schema(description = "공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "합정 메세나폴리스")
            String buildingName,

            @Schema(description = "대표 이미지 URL (사진 목록의 첫 번째)", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid1.jpg")
            String thumbnailUrl,

            @Schema(description = "등록일 (yyyy-MM-dd)", example = "2026-07-23")
            LocalDate registeredAt
    ) {}

    @Schema(description = "내 공간 목록 조회 응답")
    @Builder
    public record MyListResult(
            @Schema(description = "공간 목록")
            List<MySpace> spaces,

            @Schema(description = "내가 등록한 전체 공간 수", example = "2")
            Integer totalCount,

            @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
            Integer currentPage,

            @Schema(description = "다음 페이지 존재 여부", example = "false")
            Boolean hasNext
    ) {}

    @Schema(description = "공간 탐색 목록 항목")
    @Builder
    public record SearchSpace(
            @Schema(description = "공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "신사 어반빌딩")
            String buildingName,

            @Schema(description = "지역(구)", example = "강남구")
            String district,

            @Schema(description = "도로명 주소", example = "서울 강남구 역삼동 130-3")
            String roadAddress,

            @Schema(description = "공간 용도(카테고리). 카드 이미지 우하단 배지에 사용", example = "POPUP_STORE")
            SpaceCategory spaceCategory,

            @Schema(
                    description = "카드 하단에 노출할 키워드. (#지역명(동), #공간유형) 순서. "
                            + "동 정보가 없는 공간은 공간유형 하나만 내려간다.",
                    example = "[\"#성수동\", \"#팝업스토어\"]"
            )
            List<String> keywords,

            @Schema(description = "노출 가격(원). 일 단위 가격", example = "80000")
            Integer displayPrice,

            @Schema(description = "대표 사진 URL (사진 목록의 첫 번째). 사진이 없으면 null", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid1.jpg")
            String thumbnailUrl,

            @Schema(description = "요청자의 찜 여부. 비로그인 시 항상 false", example = "false")
            Boolean isWishlisted,

            @Schema(description = "해당 공간의 총 찜 수 (로그인 여부와 무관)", example = "20")
            Integer wishCount,

            @Schema(description = "위도 (지도 뷰 마커용)", example = "37.5012")
            Double latitude,

            @Schema(description = "경도 (지도 뷰 마커용)", example = "127.0397")
            Double longitude
    ) {}

    @Schema(description = "공간 탐색 응답")
    @Builder
    public record SearchResult(
            @Schema(description = "공간 목록")
            List<SearchSpace> spaces,

            @Schema(description = "필터 조건에 맞는 전체 공간 수 (페이지네이션 계산용)", example = "100")
            Integer totalCount,

            @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
            Integer currentPage,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            Boolean hasNext
    ) {}

    @Builder
    public record AiRecommendedSpace(
            Long spaceId,
            String buildingName,
            String tag,
            String district,
            String roadAddress,
            Double exclusiveArea,
            String basicInfo,
            Integer pricePerDay,
            Integer pricePerWeek,
            Integer pricePerMonth,
            String thumbnailUrl,
            Boolean parkingAvailable,
            Boolean isWishlisted
    ) {}

    @Builder
    public record AiRecommendedSpaceList(
            List<AiRecommendedSpace> spaces,
            Boolean hasNext,
            String nextCursor
    ) {}
}
