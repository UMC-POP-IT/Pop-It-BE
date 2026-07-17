package com.popIt.pop_it.domain.facility.converter;

import com.popIt.pop_it.domain.facility.dto.FacilityResDTO;
import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.enums.FacilityCategory;

import java.util.Arrays;
import java.util.List;

public class FacilityConverter {

    public static FacilityResDTO.ListResult toListResult(List<Facility> facilities) {
        List<FacilityResDTO.CategoryGroup> groups = Arrays.stream(FacilityCategory.values())
                .map(category -> new FacilityResDTO.CategoryGroup(
                        category.name(),
                        facilities.stream()
                                .filter(f -> f.getCategory() == category)
                                .map(f -> new FacilityResDTO.Item(
                                        f.getId(),
                                        f.getName().getDescription()
                                ))
                                .toList()
                ))
                .filter(group -> !group.items().isEmpty())
                .toList();

        return new FacilityResDTO.ListResult(groups);
    }
}
