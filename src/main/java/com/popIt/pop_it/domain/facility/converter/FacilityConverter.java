package com.popIt.pop_it.domain.facility.converter;

import com.popIt.pop_it.domain.facility.dto.FacilityResDTO;
import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.enums.FacilityCategory;

import java.util.Arrays;
import java.util.List;

public class FacilityConverter {

    public static FacilityResDTO.FacilityListRes toListResult(List<Facility> facilities) {
        List<FacilityResDTO.FacilityCategoryGroupRes> groups = Arrays.stream(FacilityCategory.values())
                .map(category -> new FacilityResDTO.FacilityCategoryGroupRes(
                        category.name(),
                        facilities.stream()
                                .filter(f -> f.getCategory() == category)
                                .map(f -> new FacilityResDTO.FacilityItemRes(
                                        f.getId(),
                                        f.getName().getDescription()
                                ))
                                .toList()
                ))
                .filter(group -> !group.items().isEmpty())
                .toList();

        return new FacilityResDTO.FacilityListRes(groups);
    }
}
