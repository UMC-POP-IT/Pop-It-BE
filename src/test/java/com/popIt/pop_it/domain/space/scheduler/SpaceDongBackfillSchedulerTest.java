package com.popIt.pop_it.domain.space.scheduler;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.space.service.KakaoLocalService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class SpaceDongBackfillSchedulerTest {

    @Autowired
    private SpaceDongBackfillScheduler scheduler;
    @Autowired
    private SpaceRepository spaceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // 실제 카카오 API를 호출하지 않고 스케줄러의 처리 로직만 검증하기 위해 목으로 대체
    @MockitoBean
    private KakaoLocalService kakaoLocalService;

    private static final Long HOST_ID = 1L;

    // 좌표별로 목 응답을 다르게 주기 위해 공간마다 다른 좌표를 사용한다
    private static final double LAT_SEONGSU = 37.5445;
    private static final double LNG_SEONGSU = 127.0557;
    private static final double LAT_YEONNAM = 37.5606;
    private static final double LNG_YEONNAM = 126.9256;

    @AfterEach
    void tearDown() {
        spaceRepository.deleteAll();
    }

    @Test
    @DisplayName("행정동이 비어 있는 공간은 재변환되어 채워진다")
    void backfill_fillsMissingDong() {
        Space space = saveSpace("성수 팝업하우스", null, LAT_SEONGSU, LNG_SEONGSU, null);
        given(kakaoLocalService.resolveDong(LAT_SEONGSU, LNG_SEONGSU)).willReturn(Optional.of("성수동"));

        scheduler.backfillMissingDong();

        assertThat(spaceRepository.findById(space.getId()).orElseThrow().getDong()).isEqualTo("성수동");
    }

    @Test
    @DisplayName("행정동이 이미 있는 공간은 대상이 아니다")
    void backfill_skipsSpaceWithDong() {
        saveSpace("연남 아트스페이스", "연남동", LAT_YEONNAM, LNG_YEONNAM, null);

        scheduler.backfillMissingDong();

        // 이미 채워진 공간 때문에 카카오를 호출하는 일이 없어야 한다
        verify(kakaoLocalService, never()).resolveDong(any(), any());
    }

    @Test
    @DisplayName("소프트 삭제된 공간은 대상이 아니다")
    void backfill_skipsDeletedSpace() {
        Space deleted = saveSpace("삭제된 공간", null, LAT_SEONGSU, LNG_SEONGSU, LocalDateTime.now());

        scheduler.backfillMissingDong();

        verify(kakaoLocalService, never()).resolveDong(any(), any());
        assertThat(spaceRepository.findById(deleted.getId()).orElseThrow().getDong()).isNull();
    }

    @Test
    @DisplayName("재시도 기간(7일)을 벗어난 오래된 공간은 대상이 아니다")
    void backfill_skipsSpaceOutsideRetryWindow() {
        Space old = saveSpace("오래된 공간", null, LAT_SEONGSU, LNG_SEONGSU, null);
        // @CreationTimestamp라 빌더로는 과거 시각을 넣을 수 없어 네이티브 쿼리로 조작
        updateCreatedAt(old.getId(), LocalDateTime.now().minusDays(30));

        scheduler.backfillMissingDong();

        verify(kakaoLocalService, never()).resolveDong(any(), any());
        assertThat(spaceRepository.findById(old.getId()).orElseThrow().getDong()).isNull();
    }

    @Test
    @DisplayName("카카오가 여전히 빈 값을 주면 행정동은 비어 있는 채로 남고 예외가 발생하지 않는다")
    void backfill_whenStillUnresolved_leavesDongNull() {
        Space space = saveSpace("변환 실패 공간", null, LAT_SEONGSU, LNG_SEONGSU, null);
        given(kakaoLocalService.resolveDong(LAT_SEONGSU, LNG_SEONGSU)).willReturn(Optional.empty());

        scheduler.backfillMissingDong();

        assertThat(spaceRepository.findById(space.getId()).orElseThrow().getDong()).isNull();
    }

    @Test
    @DisplayName("한 건이 실패해도 나머지 공간은 정상적으로 처리된다")
    void backfill_oneFailureDoesNotAffectOthers() {
        Space failing = saveSpace("실패할 공간", null, LAT_SEONGSU, LNG_SEONGSU, null);
        Space succeeding = saveSpace("성공할 공간", null, LAT_YEONNAM, LNG_YEONNAM, null);

        given(kakaoLocalService.resolveDong(LAT_SEONGSU, LNG_SEONGSU))
                .willThrow(new RuntimeException("예상치 못한 오류"));
        given(kakaoLocalService.resolveDong(LAT_YEONNAM, LNG_YEONNAM))
                .willReturn(Optional.of("연남동"));

        scheduler.backfillMissingDong();

        assertThat(spaceRepository.findById(failing.getId()).orElseThrow().getDong()).isNull();
        assertThat(spaceRepository.findById(succeeding.getId()).orElseThrow().getDong()).isEqualTo("연남동");
    }

    @Test
    @DisplayName("대상이 없으면 카카오를 호출하지 않는다")
    void backfill_withNoTargets_doesNothing() {
        scheduler.backfillMissingDong();

        verify(kakaoLocalService, never()).resolveDong(any(), any());
    }

    private void updateCreatedAt(Long spaceId, LocalDateTime createdAt) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                entityManager.createNativeQuery("update space set created_at = ?1 where id = ?2")
                        .setParameter(1, createdAt)
                        .setParameter(2, spaceId)
                        .executeUpdate()
        );
    }

    private Space saveSpace(String buildingName, String dong,
                            double latitude, double longitude, LocalDateTime deletedAt) {
        return spaceRepository.save(Space.builder()
                .buildingName(buildingName)
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울특별시")
                .district("성동구")
                .dong(dong)
                .latitude(latitude)
                .longitude(longitude)
                .roadAddress("서울특별시 성동구 테스트로 1")
                .addressDetail("101호")
                .deposit(1_000_000L)
                .pricePerDay(80_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusMonths(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(50.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .floorNumber(1)
                .parkingAvailable(true)
                .description("테스트 공간입니다.")
                .deletedAt(deletedAt)
                .hostId(HOST_ID)
                .build());
    }
}