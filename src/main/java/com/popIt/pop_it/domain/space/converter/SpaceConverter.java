package com.popIt.pop_it.domain.space.converter;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public class SpaceConverter {

    private SpaceConverter() {
    }

    public static Space toSpace(SpaceReqDTO.SpaceCreateReq request, Long hostId) {
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

    public static SpaceResDTO.SpaceCreateRes toCreateResult(Space space) {
        return SpaceResDTO.SpaceCreateRes.builder()
                .spaceId(space.getId())
                .buildingName(space.getBuildingName())
                .build();
    }

    public static SpaceResDTO.SpaceFacilityItemRes toFacilityItem(Facility facility) {
        return new SpaceResDTO.SpaceFacilityItemRes(
                facility.getId(),
                facility.getCategory(),
                facility.getName().getDescription()
        );
    }

    public static SpaceResDTO.SpaceDetailRes toDetail(
            Space space,
            List<String> imageUrls,
            List<Facility> facilities,
            boolean isMine,
            boolean isWishlisted,
            int wishCount
    ) {
        return SpaceResDTO.SpaceDetailRes.builder()
                .spaceId(space.getId())
                .buildingName(space.getBuildingName())
                .registrantType(space.getRegistrantType())
                .buildingType(space.getBuildingType())
                .city(space.getCity())
                .district(space.getDistrict())
                .roadAddress(space.getRoadAddress())
                .addressDetail(space.getAddressDetail())
                .latitude(space.getLatitude())
                .longitude(space.getLongitude())
                .deposit(space.getDeposit())
                .pricePerDay(space.getPricePerDay())
                .availableStartDate(space.getAvailableStartDate())
                .availableEndDate(space.getAvailableEndDate())
                .spaceCategory(space.getSpaceCategory())
                .spaceType(space.getSpaceType())
                .exclusiveArea(space.getExclusiveArea())
                .floorType(space.getFloorType())
                .floorNumber(space.getFloorNumber())
                .parkingAvailable(space.getParkingAvailable())
                .description(space.getDescription())
                .imageUrls(imageUrls)
                .facilities(facilities.stream()
                        .map(SpaceConverter::toFacilityItem)
                        .toList()
                )
                .isMine(isMine)
                .isWishlisted(isWishlisted)
                .wishCount(wishCount)
                .build();
    }

    public static SpaceResDTO.MySpaceListRes toMyListResult(
            Page<Space> spacePage,
            Map<Long, String> thumbnailUrlBySpaceId
    ) {
        List<SpaceResDTO.MySpaceRes> spaces = spacePage.getContent().stream()
                .map(space -> SpaceResDTO.MySpaceRes.builder()
                        .spaceId(space.getId())
                        .buildingName(space.getBuildingName())
                        .thumbnailUrl(thumbnailUrlBySpaceId.get(space.getId()))
                        .registeredAt(space.getCreatedAt().toLocalDate())
                        .build())
                .toList();

        return SpaceResDTO.MySpaceListRes.builder()
                .spaces(spaces)
                .totalCount((int) spacePage.getTotalElements())
                .currentPage(spacePage.getNumber())
                .hasNext(spacePage.hasNext())
                .build();
    }
}
