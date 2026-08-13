package com.popIt.pop_it.domain.facility.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class FacilityResDTO {
    public record FacilityListRes(
            @Schema(description = "카테고리별로 묶인 시설 목록")
            List<FacilityCategoryGroupRes> facilities
    ) {}

    public record FacilityCategoryGroupRes(
            @Schema(description = "시설 카테고리 (FacilityCategory enum 이름)", example = "HEATING_COOLING", allowableValues = {"HEATING_COOLING", "SECURITY", "ETC"})
            String category,

            @Schema(description = "해당 카테고리에 속한 시설 목록")
            List<FacilityItemRes> items
    ) {}

    public record FacilityItemRes(
            @Schema(description = "시설 ID (공간 등록/수정 시 facilityIds로 전달)", example = "1")
            Long facilityId,

            @Schema(description = "시설명", example = "에어컨")
            String name
    ) {}
}
