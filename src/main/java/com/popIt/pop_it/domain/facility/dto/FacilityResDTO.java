package com.popIt.pop_it.domain.facility.dto;

import java.util.List;

public class FacilityResDTO {
    public record FacilityListRes(
            List<FacilityCategoryGroupRes> facilities
    ) {}

    public record FacilityCategoryGroupRes(
            String category,
            List<FacilityItemRes> items
    ) {}

    public record FacilityItemRes(
            Long facilityId,
            String name
    ) {}
}
