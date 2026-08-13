package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SpaceSearchTest {

    @Autowired
    private SpaceService spaceService;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;

    private Long popupSpaceId;      // 성수동 / 성동구 / 팝업스토어 / 오픈형 홀
    private Long gallerySpaceId;    // 연남동 / 마포구 / 전시, 갤러리 / 가벽 분리형
    private Long showroomSpaceId;   // 역삼동 / 강남구 / 쇼룸 / 룸 분리형

    @BeforeEach
    void setUp() {
        popupSpaceId = saveSpace("성수 팝업하우스", "성동구", "성수동",
                SpaceCategory.POPUP_STORE, SpaceType.OPEN_HALL, null).getId();
        gallerySpaceId = saveSpace("연남 아트스페이스", "마포구", "연남동",
                SpaceCategory.EXHIBITION_GALLERY, SpaceType.PARTITION_WALL, null).getId();
        showroomSpaceId = saveSpace("역삼 쇼케이스", "강남구", "역삼동",
                SpaceCategory.SHOWROOM, SpaceType.ROOM_SEPARATED, null).getId();
    }

    // 키워드 미입력
    @Test
    @DisplayName("키워드를 입력하지 않으면 전체 공간이 조회된다")
    void search_withoutKeyword_returnsAllSpaces() {
        SpaceResDTO.SpaceSearchListRes result = search(null);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(popupSpaceId, gallerySpaceId, showroomSpaceId);
    }

    @Test
    @DisplayName("키워드가 빈 문자열이면 미입력과 동일하게 전체 공간이 조회된다")
    void search_withEmptyKeyword_returnsAllSpaces() {
        // 빈 문자열이 enum 매칭에 걸려 특정 카테고리만 나오면 안 된다
        SpaceResDTO.SpaceSearchListRes result = search("");

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(popupSpaceId, gallerySpaceId, showroomSpaceId);
    }

    @Test
    @DisplayName("키워드가 공백뿐이면 미입력과 동일하게 전체 공간이 조회된다")
    void search_withBlankKeyword_returnsAllSpaces() {
        SpaceResDTO.SpaceSearchListRes result = search("   ");

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(popupSpaceId, gallerySpaceId, showroomSpaceId);
    }

    // 매칭 실패
    @Test
    @DisplayName("어디에도 매칭되지 않는 키워드는 빈 결과를 반환한다 (전체가 조회되면 안 된다)")
    void search_withUnmatchableKeyword_returnsEmpty() {
        // 카테고리/유형 enum 매칭에 실패해도 조건이 꺼지면서 전체가 조회되는 일이 없어야 한다
        SpaceResDTO.SpaceSearchListRes result = search("존재하지않는공간유형");

        assertThat(result.spaces()).isEmpty();
        assertThat(result.totalCount()).isZero();
        assertThat(result.hasNext()).isFalse();
    }

    // 정보(카테고리 / 공간 유형) 검색
    @Test
    @DisplayName("카테고리 한글명으로 검색하면 해당 카테고리 공간만 조회된다")
    void search_byCategoryName() {
        SpaceResDTO.SpaceSearchListRes result = search("팝업스토어");

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(popupSpaceId)
                .doesNotContain(gallerySpaceId, showroomSpaceId);
    }

    @Test
    @DisplayName("카테고리 한글명 일부만 입력해도 검색된다")
    void search_byPartialCategoryName() {
        SpaceResDTO.SpaceSearchListRes result = search("팝업");

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(popupSpaceId);
    }

    @Test
    @DisplayName("공간 유형 검색 시 공백은 무시된다 (오픈형홀 = 오픈형 홀)")
    void search_bySpaceTypeIgnoringWhitespace() {
        SpaceResDTO.SpaceSearchListRes withSpace = search("오픈형 홀");
        SpaceResDTO.SpaceSearchListRes withoutSpace = search("오픈형홀");

        assertThat(withSpace.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(popupSpaceId);
        assertThat(withoutSpace.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(popupSpaceId);
    }

    // 공간명, 지역 이름 검색
    @Test
    @DisplayName("건물명 일부로 검색된다")
    void search_byBuildingName() {
        SpaceResDTO.SpaceSearchListRes result = search("아트스페이스");

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .containsExactly(gallerySpaceId);
    }

    @Test
    @DisplayName("동 이름으로 검색된다")
    void search_byDong() {
        SpaceResDTO.SpaceSearchListRes result = search("연남동");

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(gallerySpaceId)
                .doesNotContain(popupSpaceId, showroomSpaceId);
    }

    // 필터
    @Test
    @DisplayName("지역(구) 필터가 적용된다")
    void search_withDistrictFilter() {
        SpaceResDTO.SpaceSearchListRes result = spaceService.searchSpaces(
                null, new SpaceReqDTO.SpaceSearchReq(null, null, "마포구", null, null, 0, 28));

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::district)
                .containsOnly("마포구");
        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(gallerySpaceId);
    }

    @Test
    @DisplayName("검색어와 필터는 AND로 동작해 교집합이 없으면 빈 결과가 나온다")
    void search_keywordAndFilterAreAnded() {
        // 성수동 공간은 성동구에 있으므로 마포구 필터와 교집합이 없다
        SpaceResDTO.SpaceSearchListRes result = spaceService.searchSpaces(
                null, new SpaceReqDTO.SpaceSearchReq("성수동", null, "마포구", null, null, 0, 28));

        assertThat(result.spaces()).isEmpty();
    }

    // 소프트 삭제
    @Test
    @DisplayName("소프트 삭제된 공간은 조회되지 않는다")
    void search_excludesDeletedSpaces() {
        Long deletedSpaceId = saveSpace("삭제된 공간", "종로구", "종로동",
                SpaceCategory.CAFE_FNB, SpaceType.OPEN_HALL, LocalDateTime.now()).getId();

        SpaceResDTO.SpaceSearchListRes result = search(null);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .doesNotContain(deletedSpaceId);
    }

    // 비로그인
    @Test
    @DisplayName("비로그인 조회 시 isWishlisted는 항상 false다")
    void search_withoutLogin_isWishlistedIsAlwaysFalse() {
        SpaceResDTO.SpaceSearchListRes result = search(null);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::isWishlisted)
                .containsOnly(false);
    }

    // 기간 필터
    @Test
    @DisplayName("기간을 지정하지 않으면 계약 가능 기간과 무관하게 전체 공간이 조회된다")
    void search_withoutPeriod_ignoresAvailablePeriod() {
        Long pastSpaceId = saveSpaceWithPeriod("계약 종료된 공간",
                LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1)).getId();

        SpaceResDTO.SpaceSearchListRes result = search(null);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(pastSpaceId);
    }

    @Test
    @DisplayName("검색 기간이 계약 가능 기간 안에 모두 포함되면 조회된다")
    void search_withPeriodInsideAvailablePeriod_returnsSpace() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(6);
        Long spaceId = saveSpaceWithPeriod("기간 포함 공간",
                start.minusDays(5), end.plusDays(5)).getId();

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(start, end);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(spaceId);
    }

    @Test
    @DisplayName("검색 기간의 일부만 계약 가능 기간에 걸치면 조회되지 않는다")
    void search_withPeriodPartiallyOutsideAvailablePeriod_excludesSpace() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(6);
        // 계약 가능 기간이 검색 종료일 하루 전에 끝나므로 기간 전체를 빌릴 수 없다
        Long spaceId = saveSpaceWithPeriod("기간 일부만 가능한 공간",
                start.minusDays(5), end.minusDays(1)).getId();

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(start, end);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .doesNotContain(spaceId);
    }

    @Test
    @DisplayName("검색 기간 중 하루라도 예약이 겹치면 조회되지 않는다")
    void search_withPeriodPartiallyOccupied_excludesSpace() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(6);
        Space space = saveSpaceWithPeriod("중간이 예약된 공간", start.minusDays(5), end.plusDays(5));
        // 검색 기간 한가운데 하루짜리 예약
        saveReservation(space, start.plusDays(3), start.plusDays(3), ReservationStatus.PAYMENT_COMPLETED);

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(start, end);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .doesNotContain(space.getId());
    }

    @Test
    @DisplayName("예약이 검색 기간 시작일에만 걸쳐도 조회되지 않는다 (경계값)")
    void search_withReservationOverlappingStartDate_excludesSpace() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(6);
        Space space = saveSpaceWithPeriod("시작일 겹침 공간", start.minusDays(5), end.plusDays(5));
        saveReservation(space, start.minusDays(3), start, ReservationStatus.APPROVED);

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(start, end);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .doesNotContain(space.getId());
    }

    @Test
    @DisplayName("예약이 검색 기간 종료일 바로 다음 날부터면 조회된다 (경계값)")
    void search_withReservationRightAfterEndDate_returnsSpace() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(6);
        Space space = saveSpaceWithPeriod("종료일 직후 예약 공간", start.minusDays(5), end.plusDays(10));
        saveReservation(space, end.plusDays(1), end.plusDays(5), ReservationStatus.PAYMENT_COMPLETED);

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(start, end);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(space.getId());
    }

    @Test
    @DisplayName("승인 대기 예약도 기간을 선점한다")
    void search_withPendingReservation_excludesSpace() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(6);
        Space space = saveSpaceWithPeriod("승인 대기 공간", start.minusDays(5), end.plusDays(5));
        saveReservation(space, start, end, ReservationStatus.PENDING_APPROVAL);

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(start, end);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .doesNotContain(space.getId());
    }

    @Test
    @DisplayName("취소된 예약은 기간을 선점하지 않는다")
    void search_withCancelledReservation_returnsSpace() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(6);
        Space space = saveSpaceWithPeriod("취소 예약만 있는 공간", start.minusDays(5), end.plusDays(5));
        saveReservation(space, start, end, ReservationStatus.CANCELLED);

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(start, end);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(space.getId());
    }

    @Test
    @DisplayName("시작일과 종료일이 같은 하루짜리 기간도 조회된다")
    void search_withSingleDayPeriod_returnsSpace() {
        LocalDate day = LocalDate.now().plusDays(10);
        Space space = saveSpaceWithPeriod("하루 검색 공간", day.minusDays(5), day.plusDays(5));

        SpaceResDTO.SpaceSearchListRes result = searchByPeriod(day, day);

        assertThat(result.spaces()).extracting(SpaceResDTO.SpaceSearchRes::spaceId)
                .contains(space.getId());
    }

    @Test
    @DisplayName("기간 필터는 키워드·지역 필터와 AND로 동작한다")
    void search_periodIsAndedWithOtherFilters() {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        saveSpaceWithPeriod("성수 기간테스트", start.minusDays(1), end.plusDays(1));

        // 성수 기간테스트 공간은 성동구에 있으므로 마포구 필터와 교집합이 없다
        SpaceResDTO.SpaceSearchListRes result = spaceService.searchSpaces(
                null, new SpaceReqDTO.SpaceSearchReq("성수 기간테스트", null, "마포구", start, end, 0, 28));

        assertThat(result.spaces()).isEmpty();
    }

    private SpaceResDTO.SpaceSearchListRes searchByPeriod(LocalDate startDate, LocalDate endDate) {
        return spaceService.searchSpaces(
                null, new SpaceReqDTO.SpaceSearchReq(null, null, null, startDate, endDate, 0, 28));
    }

    private Space saveSpaceWithPeriod(String buildingName, LocalDate availableStartDate, LocalDate availableEndDate) {
        return spaceRepository.save(Space.builder()
                .buildingName(buildingName)
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울특별시")
                .district("성동구")
                .dong("성수동")
                .latitude(37.5445)
                .longitude(127.0557)
                .roadAddress("서울 성동구 테스트로 1")
                .addressDetail("101호")
                .deposit(1_000_000L)
                .pricePerDay(80_000)
                .availableStartDate(availableStartDate)
                .availableEndDate(availableEndDate)
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(50.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .floorNumber(1)
                .parkingAvailable(true)
                .description("테스트 공간입니다.")
                .hostId(1L)
                .build());
    }

    private void saveReservation(Space space, LocalDate startDate, LocalDate endDate, ReservationStatus status) {
        User guest = userRepository.save(User.builder()
                .socialProvider(SocialProvider.GOOGLE)
                .socialUid("guest-" + UUID.randomUUID())
                .nickname("게스트")
                .currentMode(UserMode.GUEST)
                .build());

        reservationRepository.save(Reservation.builder()
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .usagePurpose("테스트 예약")
                .rentalFee(400_000L)
                .deposit(500_000L)
                .insuranceFee(20_000L)
                .platformFee(40_000L)
                .totalPrice(960_000L)
                .space(space)
                .user(guest)
                .build());
    }

    private SpaceResDTO.SpaceSearchListRes search(String keyword) {
        return spaceService.searchSpaces(null, new SpaceReqDTO.SpaceSearchReq(keyword, null, null, null, null, 0, 28));
    }

    private Space saveSpace(String buildingName, String district, String dong,
                            SpaceCategory spaceCategory, SpaceType spaceType, LocalDateTime deletedAt) {
        return spaceRepository.save(Space.builder()
                .buildingName(buildingName)
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울특별시")
                .district(district)
                .dong(dong)
                .latitude(37.5445)
                .longitude(127.0557)
                .roadAddress("서울 " + district + " 테스트로 1")
                .addressDetail("101호")
                .deposit(1_000_000L)
                .pricePerDay(80_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusMonths(1))
                .spaceCategory(spaceCategory)
                .spaceType(spaceType)
                .exclusiveArea(50.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .floorNumber(1)
                .parkingAvailable(true)
                .description("테스트 공간입니다.")
                .deletedAt(deletedAt)
                .hostId(1L)
                .build());
    }
}