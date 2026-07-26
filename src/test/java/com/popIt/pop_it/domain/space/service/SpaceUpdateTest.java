package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.repository.FacilityRepository;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceFacilityRepository;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
class SpaceUpdateTest {

    @Autowired private SpaceService spaceService;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private SpaceImageRepository spaceImageRepository;
    @Autowired private SpaceFacilityRepository spaceFacilityRepository;
    @Autowired private FacilityRepository facilityRepository;

    // 좌표를 바꾸면 서비스가 카카오 로컬 API로 동을 다시 계산
    // 테스트가 실제 네트워크에 의존하지 않도록 이 빈만 가짜로 대체
    @MockitoBean private KakaoLocalService kakaoLocalService;

    private static final Long HOST_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Test
    @DisplayName("전달한 필드만 수정되고 나머지는 유지된다")
    void update_onlyGivenFieldsChange() {
        Long spaceId = saveSpace(HOST_ID).getId();

        spaceService.updateSpace(HOST_ID, spaceId, update().pricePerDay(120_000).build());

        Space updated = reload(spaceId);
        assertThat(updated.getPricePerDay()).isEqualTo(120_000);
        assertThat(updated.getBuildingName()).isEqualTo("합정 메세나폴리스");
        assertThat(updated.getDeposit()).isEqualTo(4_500_000L);
    }

    @Test
    @DisplayName("위도만 보내면 400 (위경도는 한 세트)")
    void update_latitudeOnly_throws() {
        Long spaceId = saveSpace(HOST_ID).getId();

        assertThatThrownBy(() ->
                spaceService.updateSpace(HOST_ID, spaceId, update().latitude(37.6).build()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.INVALID_COORDINATE_PAIR);
    }

    @Test
    @DisplayName("위경도를 함께 보내면 좌표가 갱신되고 동이 다시 계산된다")
    void update_bothCoordinates_recalculatesDong() {
        Long spaceId = saveSpace(HOST_ID).getId();
        given(kakaoLocalService.resolveDong(anyDouble(), anyDouble())).willReturn(Optional.of("성수동"));

        spaceService.updateSpace(HOST_ID, spaceId,
                update().latitude(37.5442).longitude(127.0494).build());

        Space updated = reload(spaceId);
        assertThat(updated.getLatitude()).isEqualTo(37.5442);
        assertThat(updated.getLongitude()).isEqualTo(127.0494);
        assertThat(updated.getDong()).isEqualTo("성수동");
    }

    @Test
    @DisplayName("층타입 없이 층수만 보내면 층 정보는 유지된다")
    void update_floorNumberOnly_keepsFloorInfo() {
        Long spaceId = saveSpace(HOST_ID).getId();

        spaceService.updateSpace(HOST_ID, spaceId, update().floorNumber(99).build());

        Space updated = reload(spaceId);
        assertThat(updated.getFloorType()).isEqualTo(FloorType.GENERAL_FLOOR);
        assertThat(updated.getFloorNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("층타입을 보내면 층수도 반영되며 null이면 층수가 지워진다")
    void update_floorTypeWithNullNumber_clearsFloorNumber() {
        Long spaceId = saveSpace(HOST_ID).getId();

        spaceService.updateSpace(HOST_ID, spaceId,
                update().floorType(FloorType.ROOFTOP).floorNumber(null).build());

        Space updated = reload(spaceId);
        assertThat(updated.getFloorType()).isEqualTo(FloorType.ROOFTOP);
        assertThat(updated.getFloorNumber()).isNull();
    }

    @Test
    @DisplayName("시설 목록을 보내면 기존 연결을 지우고 새 목록으로 교체한다")
    void update_replacesFacilities() {
        Space space = saveSpace(HOST_ID);
        List<Facility> facilities = facilityRepository.findAll();
        Long oldFacility = facilities.get(0).getId();
        Long newFacilityA = facilities.get(1).getId();
        Long newFacilityB = facilities.get(2).getId();
        linkFacility(space, facilities.get(0)); // 기존엔 oldFacility 하나만 연결

        spaceService.updateSpace(HOST_ID, space.getId(),
                update().facilityIds(List.of(newFacilityA, newFacilityB)).build());

        List<Long> linked = spaceFacilityRepository.findFacilitiesBySpaceId(space.getId())
                .stream().map(Facility::getId).toList();
        assertThat(linked).containsExactlyInAnyOrder(newFacilityA, newFacilityB)
                .doesNotContain(oldFacility);
    }

    @Test
    @DisplayName("존재하지 않는 시설이 섞이면 400이고 기존 시설은 지워지지 않는다")
    void update_withUnknownFacility_throwsAndKeepsExisting() {
        Space space = saveSpace(HOST_ID);
        List<Facility> facilities = facilityRepository.findAll();
        Long existing = facilities.get(0).getId();
        linkFacility(space, facilities.get(0));
        Long unknownId = 999_999L;

        assertThatThrownBy(() -> spaceService.updateSpace(HOST_ID, space.getId(),
                update().facilityIds(List.of(existing, unknownId)).build()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.FACILITY_NOT_FOUND);

        // 검증이 삭제보다 앞서므로 기존 연결이 그대로 남아 있어야 한다
        List<Long> linked = spaceFacilityRepository.findFacilitiesBySpaceId(space.getId())
                .stream().map(Facility::getId).toList();
        assertThat(linked).containsExactly(existing);
    }

    @Test
    @DisplayName("시설을 빈 배열로 보내면 전부 해제된다")
    void update_withEmptyFacilities_removesAll() {
        Space space = saveSpace(HOST_ID);
        List<Facility> facilities = facilityRepository.findAll();
        linkFacility(space, facilities.get(0));

        spaceService.updateSpace(HOST_ID, space.getId(), update().facilityIds(List.of()).build());

        assertThat(spaceFacilityRepository.findFacilitiesBySpaceId(space.getId())).isEmpty();
    }

    @Test
    @DisplayName("사진 목록을 보내면 기존을 지우고 배열 순서대로 교체한다")
    void update_replacesImagesPreservingOrder() {
        Space space = saveSpace(HOST_ID);
        saveImage(space, "https://s3/old1.jpg", 0);
        saveImage(space, "https://s3/old2.jpg", 1);

        List<String> newUrls = List.of("https://s3/new1.jpg", "https://s3/new2.jpg", "https://s3/new3.jpg");
        spaceService.updateSpace(HOST_ID, space.getId(), update().imageUrls(newUrls).build());

        // findImageUrlsBySpaceId는 sortOrder 오름차순으로 반환
        List<String> stored = spaceImageRepository.findImageUrlsBySpaceId(space.getId());
        assertThat(stored).containsExactly("https://s3/new1.jpg", "https://s3/new2.jpg", "https://s3/new3.jpg");
    }

    @Test
    @DisplayName("계약 시작일이 종료일보다 늦으면 400")
    void update_invalidDateRange_throws() {
        Long spaceId = saveSpace(HOST_ID).getId();

        assertThatThrownBy(() -> spaceService.updateSpace(HOST_ID, spaceId,
                update()
                        .availableStartDate(LocalDate.of(2026, 12, 31))
                        .availableEndDate(LocalDate.of(2026, 6, 1))
                        .build()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.INVALID_AVAILABLE_DATE_RANGE);
    }

    @Test
    @DisplayName("종료일만 수정해도 기존 시작일과 비교해 검증한다")
    void update_onlyEndDate_validatedAgainstExistingStart() {
        Long spaceId = saveSpace(HOST_ID).getId(); // 기존 시작일 2026-06-01

        assertThatThrownBy(() -> spaceService.updateSpace(HOST_ID, spaceId,
                update().availableEndDate(LocalDate.of(2026, 1, 1)).build()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.INVALID_AVAILABLE_DATE_RANGE);
    }

    @Test
    @DisplayName("존재하지 않는 공간이면 404")
    void update_spaceNotFound_throws() {
        assertThatThrownBy(() ->
                spaceService.updateSpace(HOST_ID, 999_999L, update().pricePerDay(120_000).build()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.SPACE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인이 등록한 공간이 아니면 403")
    void update_notOwner_throws() {
        Long spaceId = saveSpace(HOST_ID).getId();

        assertThatThrownBy(() ->
                spaceService.updateSpace(OTHER_USER_ID, spaceId, update().pricePerDay(120_000).build()))
                .isInstanceOf(ProjectException.class)
                .extracting(e -> ((ProjectException) e).getErrorCode())
                .isEqualTo(SpaceErrorCode.NOT_SPACE_OWNER);
    }

    // 헬퍼
    private Space reload(Long spaceId) {
        return spaceRepository.findById(spaceId).orElseThrow();
    }

    private Space saveSpace(Long hostId) {
        return spaceRepository.save(Space.builder()
                .buildingName("합정 메세나폴리스")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.LARGE_OFFICE)
                .city("서울특별시")
                .district("마포구")
                .dong("합정동")
                .latitude(37.5012)
                .longitude(127.0397)
                .roadAddress("서울특별시 마포구 합정동 130-3")
                .addressDetail("302동 302호")
                .deposit(4_500_000L)
                .pricePerDay(90_000)
                .availableStartDate(LocalDate.of(2026, 6, 1))
                .availableEndDate(LocalDate.of(2026, 12, 31))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(66.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .floorNumber(2)
                .parkingAvailable(true)
                .description("합정 중심지에 위치한 공간입니다.")
                .hostId(hostId)
                .build());
    }

    private void saveImage(Space space, String url, int sortOrder) {
        spaceImageRepository.save(SpaceImage.builder()
                .space(space).imageUrl(url).sortOrder(sortOrder).build());
    }

    private void linkFacility(Space space, Facility facility) {
        spaceFacilityRepository.save(SpaceFacility.builder()
                .space(space).facility(facility).build());
    }

    private UpdateBuilder update() {
        return new UpdateBuilder();
    }

    private static class UpdateBuilder {
        private String buildingName;
        private RegistrantType registrantType;
        private BuildingType buildingType;
        private String city;
        private String district;
        private String roadAddress;
        private String addressDetail;
        private Double latitude;
        private Double longitude;
        private Long deposit;
        private Integer pricePerDay;
        private LocalDate availableStartDate;
        private LocalDate availableEndDate;
        private SpaceCategory spaceCategory;
        private SpaceType spaceType;
        private Double exclusiveArea;
        private FloorType floorType;
        private Integer floorNumber;
        private Boolean parkingAvailable;
        private String description;
        private List<Long> facilityIds;
        private List<String> imageUrls;

        UpdateBuilder pricePerDay(Integer v) { this.pricePerDay = v; return this; }
        UpdateBuilder latitude(Double v) { this.latitude = v; return this; }
        UpdateBuilder longitude(Double v) { this.longitude = v; return this; }
        UpdateBuilder floorType(FloorType v) { this.floorType = v; return this; }
        UpdateBuilder floorNumber(Integer v) { this.floorNumber = v; return this; }
        UpdateBuilder availableStartDate(LocalDate v) { this.availableStartDate = v; return this; }
        UpdateBuilder availableEndDate(LocalDate v) { this.availableEndDate = v; return this; }
        UpdateBuilder facilityIds(List<Long> v) { this.facilityIds = v; return this; }
        UpdateBuilder imageUrls(List<String> v) { this.imageUrls = v; return this; }

        SpaceReqDTO.SpaceUpdateReq build() {
            return new SpaceReqDTO.SpaceUpdateReq(
                    buildingName, registrantType, buildingType, city, district,
                    roadAddress, addressDetail, latitude, longitude, deposit,
                    pricePerDay, availableStartDate, availableEndDate, spaceCategory, spaceType,
                    exclusiveArea, floorType, floorNumber, parkingAvailable, description,
                    facilityIds, imageUrls
            );
        }
    }
}