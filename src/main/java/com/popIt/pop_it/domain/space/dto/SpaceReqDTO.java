package com.popIt.pop_it.domain.space.dto;

import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class SpaceReqDTO {

    public record Create(
            @NotBlank
            @Size(max = 20)
            String buildingName,

            @NotNull
            RegistrantType registrantType,

            @NotNull
            BuildingType buildingType,

            @NotBlank
            @Size(max = 50)
            String city,

            @NotBlank
            @Size(max = 50)
            String district,

            @NotBlank
            String roadAddress,

            @NotBlank
            @Size(max = 30)
            String addressDetail,

            @NotNull
            Double latitude,

            @NotNull
            Double longitude,

            @NotNull
            @PositiveOrZero
            Long deposit,

            @Positive
            Integer pricePerDay,

            @Positive
            Integer pricePerWeek,

            @Positive
            Integer pricePerMonth,

            @NotNull
            LocalDate availableStartDate,

            @NotNull
            LocalDate availableEndDate,

            @NotNull
            SpaceCategory spaceCategory,

            @NotNull
            SpaceType spaceType,

            @NotNull
            @Positive
            Double exclusiveArea,

            @NotNull
            FloorType floorType,

            Integer floorNumber,

            @NotNull
            Boolean parkingAvailable,

            @NotBlank
            @Size(max = 1000)
            String description,

            List<Long> facilityIds,

            @NotEmpty
            List<@NotBlank String> imageUrls
    ) {}
}