package com.popIt.pop_it.domain.facility.controller;

import com.popIt.pop_it.domain.facility.dto.FacilityResDTO;
import com.popIt.pop_it.domain.facility.exception.FacilitySuccessCode;
import com.popIt.pop_it.domain.facility.service.FacilityService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping
    public ApiResponse<FacilityResDTO.FacilityListRes> getFacilities() {
        FacilityResDTO.FacilityListRes result = facilityService.getFacilities();
        return ApiResponse.onSuccess(FacilitySuccessCode.FACILITY_LIST_FETCHED, result);
    }
}