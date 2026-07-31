package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RealtimeRecommendType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SpaceRealtimeRecommendTest {

    @Autowired
    private SpaceService spaceService;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private SpaceImageRepository spaceImageRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("등록 30일 이내 공간은 신규 공간 문구로 노출된다")
    void newSpace_hasColdStartMent() {
        Space newSpace = saveSpace("성수 신규", "성동구", "성수동", monthStart(), monthEnd());

        SpaceResDTO.SpaceRealtimeRecommendedRes card =
                findCard(spaceService.getRealtimeRecommendedSpaces(), newSpace.getId());

        assertThat(RealtimeRecommendType.NEW.getTitles()).contains(card.title());
        assertThat(RealtimeRecommendType.NEW.getSubtitles()).contains(card.subtitle());
    }

    @Test
    @DisplayName("오래된 공간이 당월에 결제된 예약이 없으면 가동률 하위 문구로 노출된다")
    void oldSpaceWithNoPaidReservation_hasLowUtilizationMent() {
        Space oldSpace = saveSpace("성수 유휴", "성동구", "성수동", monthStart(), monthEnd());
        makeOld(oldSpace.getId(), 60);

        SpaceResDTO.SpaceRealtimeRecommendedRes card =
                findCard(spaceService.getRealtimeRecommendedSpaces(), oldSpace.getId());

        assertThat(RealtimeRecommendType.LOW_UTILIZATION.getTitles()).contains(card.title());
    }

    @Test
    @DisplayName("당월에 대여 가능 기간이 없는 오래된 공간은 기본 공간 문구로 노출된다")
    void oldSpaceNotAvailableThisMonth_hasDefaultMent() {
        // 예약률을 계산할 분모가 없는데 가동률 하위로 잘못 분류되면 안 된다
        Space oldSpace = saveSpace("과거 종료 공간", "마포구", "연남동",
                LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5));
        makeOld(oldSpace.getId(), 60);

        SpaceResDTO.SpaceRealtimeRecommendedRes card =
                findCard(spaceService.getRealtimeRecommendedSpaces(), oldSpace.getId());

        assertThat(RealtimeRecommendType.DEFAULT.getSubtitles()).contains(card.subtitle());
        assertThat(card.title())
                .doesNotContain(RealtimeRecommendType.REGION_PLACEHOLDER)
                .doesNotContain("null");
    }

    @Test
    @DisplayName("가동률 하위와 신규 공간이 기본 공간보다 앞에 배치된다")
    void frontSlots_arePrioritized() {
        Space defaultSpace = saveSpace("기본 공간", "마포구", "연남동",
                LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5));
        makeOld(defaultSpace.getId(), 60);

        Space lowUtilizationSpace = saveSpace("유휴 공간", "성동구", "성수동", monthStart(), monthEnd());
        makeOld(lowUtilizationSpace.getId(), 60);

        Space newSpace = saveSpace("신규 공간", "강남구", "역삼동", monthStart(), monthEnd());

        List<Long> spaceIds = spaceService.getRealtimeRecommendedSpaces().spaces().stream()
                .map(SpaceResDTO.SpaceRealtimeRecommendedRes::spaceId)
                .toList();

        assertThat(spaceIds.indexOf(lowUtilizationSpace.getId()))
                .isLessThan(spaceIds.indexOf(defaultSpace.getId()));
        assertThat(spaceIds.indexOf(newSpace.getId()))
                .isLessThan(spaceIds.indexOf(defaultSpace.getId()));
    }

    @Test
    @DisplayName("삭제된 공간은 추천 목록에서 제외된다")
    void deletedSpace_isExcluded() {
        Space deletedSpace = saveSpace("삭제된 공간", "성동구", "성수동", monthStart(), monthEnd());
        deleteSpace(deletedSpace.getId());

        assertThat(spaceService.getRealtimeRecommendedSpaces().spaces())
                .extracting(SpaceResDTO.SpaceRealtimeRecommendedRes::spaceId)
                .doesNotContain(deletedSpace.getId());
    }

    @Test
    @DisplayName("대표 이미지는 노출 순서가 가장 앞선 사진이 내려간다")
    void thumbnail_isFirstImage() {
        Space space = saveSpace("사진 있는 공간", "성동구", "성수동", monthStart(), monthEnd());
        saveImage(space, "https://example.com/second.jpg", 2);
        saveImage(space, "https://example.com/first.jpg", 1);

        SpaceResDTO.SpaceRealtimeRecommendedRes card =
                findCard(spaceService.getRealtimeRecommendedSpaces(), space.getId());

        assertThat(card.thumbnailUrl()).isEqualTo("https://example.com/first.jpg");
    }

    @Test
    @DisplayName("공간이 하나도 없으면 빈 목록을 반환한다")
    void noSpaces_returnsEmptyList() {
        spaceImageRepository.deleteAll();
        spaceRepository.deleteAll();
        spaceRepository.flush();

        assertThat(spaceService.getRealtimeRecommendedSpaces().spaces()).isEmpty();
    }

    private SpaceResDTO.SpaceRealtimeRecommendedRes findCard(
            SpaceResDTO.SpaceRealtimeRecommendedListRes result, Long spaceId) {
        return result.spaces().stream()
                .filter(card -> card.spaceId().equals(spaceId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("추천 목록에 공간(id=" + spaceId + ")이 없습니다."));
    }

    // createdAt이 @CreationTimestamp(updatable = false)라 JPA로는 못 바꿔서 네이티브 쿼리로 과거로 민다
    private void makeOld(Long spaceId, int days) {
        entityManager.createNativeQuery("update space set created_at = ?1 where id = ?2")
                .setParameter(1, LocalDateTime.now().minusDays(days))
                .setParameter(2, spaceId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private void deleteSpace(Long spaceId) {
        entityManager.createNativeQuery("update space set deleted_at = ?1 where id = ?2")
                .setParameter(1, LocalDateTime.now())
                .setParameter(2, spaceId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private LocalDate monthStart() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private LocalDate monthEnd() {
        return LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1);
    }

    private Space saveSpace(String buildingName, String district, String dong,
                            LocalDate availableStart, LocalDate availableEnd) {
        return spaceRepository.saveAndFlush(Space.builder()
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
                .availableStartDate(availableStart)
                .availableEndDate(availableEnd)
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

    private void saveImage(Space space, String imageUrl, int sortOrder) {
        spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .space(space)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build());
    }
}