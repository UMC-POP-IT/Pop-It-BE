package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SpaceSearchTest {

    @Autowired
    private SpaceService spaceService;
    @Autowired
    private SpaceRepository spaceRepository;

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
                null, new SpaceReqDTO.SpaceSearchReq(null, null, "마포구", 0, 28));

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
                null, new SpaceReqDTO.SpaceSearchReq("성수동", null, "마포구", 0, 28));

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

    private SpaceResDTO.SpaceSearchListRes search(String keyword) {
        return spaceService.searchSpaces(null, new SpaceReqDTO.SpaceSearchReq(keyword, null, null, 0, 28));
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