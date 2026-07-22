package com.popIt.pop_it.domain.space.converter;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import com.popIt.pop_it.domain.space.entity.SpaceImage;

public class SpaceConverter {

    private SpaceConverter() {
    }

    public static Space toSpace(SpaceReqDTO.Create request, Long hostId) {
        return Space.builder()
                .buildingName(request.buildingName())
                .registrantType(request.registrantType())
                .buildingType(request.buildingType())
                .city(request.city())
                .district(request.district())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .roadAddress(request.roadAddress())
                .addressDetail(request.addressDetail())
                .deposit(request.deposit())
                .pricePerDay(request.pricePerDay())
                .pricePerWeek(request.pricePerWeek())
                .pricePerMonth(request.pricePerMonth())
                .availableStartDate(request.availableStartDate())
                .availableEndDate(request.availableEndDate())
                .spaceCategory(request.spaceCategory())
                .spaceType(request.spaceType())
                .exclusiveArea(request.exclusiveArea())
                .floorType(request.floorType())
                .floorNumber(request.floorNumber())
                .parkingAvailable(request.parkingAvailable())
                .description(request.description())
                .hostId(hostId)
                .build();
    }

    public static SpaceImage toSpaceImage(Space space, String imageUrl, int sortOrder) {
        return SpaceImage.builder()
                .space(space)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }

    public static SpaceFacility toSpaceFacility(Space space, Facility facility) {
        return SpaceFacility.builder()
                .space(space)
                .facility(facility)
                .build();
    }

    public static SpaceResDTO.CreateResult toCreateResult(Space space) {
        return SpaceResDTO.CreateResult.builder()
                .spaceId(space.getId())
                .buildingName(space.getBuildingName())
                .build();
    }
}
