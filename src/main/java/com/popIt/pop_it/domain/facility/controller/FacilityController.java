package com.popIt.pop_it.domain.facility.controller;

import com.popIt.pop_it.domain.facility.dto.FacilityResDTO;
import com.popIt.pop_it.domain.facility.exception.FacilitySuccessCode;
import com.popIt.pop_it.domain.facility.service.FacilityService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시설", description = "공간 등록/수정 시 선택 가능한 시설 목록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    @Operation(summary = "시설 목록 조회", description = "카테고리별로 그룹핑된 전체 시설 목록을 조회합니다. 공간 등록/수정 시 facilityIds로 전달할 값을 여기서 가져옵니다.")
    @GetMapping
    public ApiResponse<FacilityResDTO.FacilityListRes> getFacilities() {
        FacilityResDTO.FacilityListRes result = facilityService.getFacilities();
        return ApiResponse.onSuccess(FacilitySuccessCode.FACILITY_LIST_FETCHED, result);
    }
}