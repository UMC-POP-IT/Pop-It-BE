package com.popIt.pop_it.domain.space.converter;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpaceConverter {

    private SpaceConverter() {
    }

    public static Space toSpace(SpaceReqDTO.Create request, Long hostId, String dong) {
        return Space.builder()
                .buildingName(request.buildingName())
                .registrantType(request.registrantType())
                .buildingType(request.buildingType())
                .city(request.city())
                .district(request.district())
                .dong(dong)
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

    public static SpaceResDTO.CreateResult toCreateResult(Space space) {
        return SpaceResDTO.CreateResult.builder()
                .spaceId(space.getId())
                .buildingName(space.getBuildingName())
                .build();
    }

    public static SpaceResDTO.FacilityItem toFacilityItem(Facility facility) {
        return new SpaceResDTO.FacilityItem(
                facility.getId(),
                facility.getCategory(),
                facility.getName().getDescription()
        );
    }

    public static SpaceResDTO.Detail toDetail(
            Space space,
            List<String> imageUrls,
            List<Facility> facilities,
            boolean isMine,
            boolean isWishlisted,
            int wishCount
    ) {
        return SpaceResDTO.Detail.builder()
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

    public static SpaceResDTO.MyListResult toMyListResult(
            Page<Space> spacePage,
            Map<Long, String> thumbnailUrlBySpaceId
    ) {
        List<SpaceResDTO.MySpace> spaces = spacePage.getContent().stream()
                .map(space -> SpaceResDTO.MySpace.builder()
                        .spaceId(space.getId())
                        .buildingName(space.getBuildingName())
                        .thumbnailUrl(thumbnailUrlBySpaceId.get(space.getId()))
                        .registeredAt(space.getCreatedAt().toLocalDate())
                        .build())
                .toList();

        return SpaceResDTO.MyListResult.builder()
                .spaces(spaces)
                .totalCount((int) spacePage.getTotalElements())
                .currentPage(spacePage.getNumber())
                .hasNext(spacePage.hasNext())
                .build();
    }

    public static SpaceResDTO.SpaceSearchListRes toSearchResult(
            Page<Space> spacePage,
            Map<Long, String> thumbnailUrlBySpaceId,
            Map<Long, Integer> wishCountBySpaceId,
            Set<Long> wishlistedSpaceIds
    ) {
        List<SpaceResDTO.SpaceSearchRes> spaces = spacePage.getContent().stream()
                .map(space -> SpaceResDTO.SpaceSearchRes.builder()
                        .spaceId(space.getId())
                        .buildingName(space.getBuildingName())
                        .district(space.getDistrict())
                        .roadAddress(space.getRoadAddress())
                        .spaceCategory(space.getSpaceCategory())
                        .keywords(toKeywords(space))
                        .displayPrice(space.getPricePerDay())
                        .thumbnailUrl(thumbnailUrlBySpaceId.get(space.getId()))
                        .isWishlisted(wishlistedSpaceIds.contains(space.getId()))
                        .wishCount(wishCountBySpaceId.getOrDefault(space.getId(), 0))
                        .latitude(space.getLatitude())
                        .longitude(space.getLongitude())
                        .build())
                .toList();

        return SpaceResDTO.SpaceSearchListRes.builder()
                .spaces(spaces)
                .totalCount((int) spacePage.getTotalElements())
                .currentPage(spacePage.getNumber())
                .hasNext(spacePage.hasNext())
                .build();
    }

    private static List<String> toKeywords(Space space) {
        List<String> keywords = new ArrayList<>();

        if (space.getDong() != null && !space.getDong().isEmpty()) {
            keywords.add("#" + space.getDong());
        }

        keywords.add("#" + space.getSpaceCategory().getDescription());

        return keywords;
    }

    public static SpaceResDTO.UpdateResult toUpdateResult(Space space) {
        return SpaceResDTO.UpdateResult.builder()
                .spaceId(space.getId())
                .build();
    }

    public static SpaceResDTO.DeleteResult toDeleteResult(Space space) {
        return SpaceResDTO.DeleteResult.builder()
                .spaceId(space.getId())
                .build();
    }
}
