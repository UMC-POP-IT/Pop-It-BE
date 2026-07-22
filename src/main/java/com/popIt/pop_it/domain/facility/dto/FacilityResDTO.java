package com.popIt.pop_it.domain.facility.dto;

import java.util.List;

public class FacilityResDTO {
    public record ListResult(
            List<CategoryGroup> facilities
    ) {}

    public record CategoryGroup(
            String category,
            List<Item> items
    ) {}

    public record Item(
            Long facilityId,
            String name
    ) {}
}
