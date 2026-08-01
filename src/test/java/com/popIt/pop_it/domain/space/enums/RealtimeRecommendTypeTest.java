package com.popIt.pop_it.domain.space.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeRecommendTypeTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 31, 12, 0);

    // 유형 판정
    @Test
    @DisplayName("등록 30일 이내면 신규 공간으로 분류된다")
    void classify_recentlyCreated_isNew() {
        assertThat(RealtimeRecommendType.classify(NOW.minusDays(3), NOW, false))
                .isEqualTo(RealtimeRecommendType.NEW);
    }

    @Test
    @DisplayName("신규 공간은 가동률 하위 조건에 걸려도 신규로 분류된다")
    void classify_newSpace_takesPrecedenceOverLowUtilization() {
        // 갓 등록된 공간은 예약 실적이 없는 게 당연해 가동률 조건에도 걸린다.
        // 가동률을 먼저 보면 콜드 스타트 문구가 아예 노출되지 않으므로 신규가 우선이어야 한다.
        assertThat(RealtimeRecommendType.classify(NOW.minusDays(3), NOW, true))
                .isEqualTo(RealtimeRecommendType.NEW);
    }

    @Test
    @DisplayName("30일이 지난 공간이 가동률 하위면 가동률 하위로 분류된다")
    void classify_oldAndLowUtilization() {
        assertThat(RealtimeRecommendType.classify(NOW.minusDays(60), NOW, true))
                .isEqualTo(RealtimeRecommendType.LOW_UTILIZATION);
    }

    @Test
    @DisplayName("30일이 지났고 가동률 조건에도 해당하지 않으면 기본 공간이다")
    void classify_oldAndNormal_isDefault() {
        assertThat(RealtimeRecommendType.classify(NOW.minusDays(60), NOW, false))
                .isEqualTo(RealtimeRecommendType.DEFAULT);
    }

    @Test
    @DisplayName("등록 30일 경계에서 29일은 신규, 31일은 신규가 아니다")
    void classify_boundary() {
        assertThat(RealtimeRecommendType.classify(NOW.minusDays(29), NOW, false))
                .isEqualTo(RealtimeRecommendType.NEW);
        // "30일 이내"는 초과 비교(isAfter)라 정확히 30일 전은 포함되지 않는다
        assertThat(RealtimeRecommendType.classify(NOW.minusDays(30), NOW, false))
                .isNotEqualTo(RealtimeRecommendType.NEW);
        assertThat(RealtimeRecommendType.classify(NOW.minusDays(31), NOW, false))
                .isNotEqualTo(RealtimeRecommendType.NEW);
    }

    @Test
    @DisplayName("등록일이 없으면 신규로 보지 않는다")
    void classify_nullCreatedAt_isNotNew() {
        assertThat(RealtimeRecommendType.classify(null, NOW, false))
                .isEqualTo(RealtimeRecommendType.DEFAULT);
    }

    // 앞단 슬롯 대상
    @Test
    @DisplayName("가동률 하위와 신규만 앞단 슬롯 대상이다")
    void isFrontSlotType() {
        assertThat(RealtimeRecommendType.LOW_UTILIZATION.isFrontSlotType()).isTrue();
        assertThat(RealtimeRecommendType.NEW.isFrontSlotType()).isTrue();
        assertThat(RealtimeRecommendType.DEFAULT.isFrontSlotType()).isFalse();
    }

    // 문구 선택
    @Test
    @DisplayName("타이틀의 [지역] 자리에 실제 지역명이 채워진다")
    void pickTitle_replacesRegionPlaceholder() {
        boolean anyReplaced = false;

        for (long spaceId = 0; spaceId < 10; spaceId++) {
            String title = RealtimeRecommendType.DEFAULT.pickTitle(spaceId, "성수동");
            if (title.contains("성수동")) {
                anyReplaced = true;
            }
            assertThat(title).doesNotContain(RealtimeRecommendType.REGION_PLACEHOLDER);
        }

        assertThat(anyReplaced).isTrue();
    }

    @Test
    @DisplayName("지역명이 없으면 [지역]이 들어간 문구는 후보에서 제외된다")
    void pickTitle_withoutRegion_excludesPlaceholderTitles() {
        // 지역명을 못 구했을 때 "이번 주 null 공간" 같은 문구가 나가면 안 된다
        for (long spaceId = 0; spaceId < 10; spaceId++) {
            assertThat(RealtimeRecommendType.DEFAULT.pickTitle(spaceId, null))
                    .doesNotContain(RealtimeRecommendType.REGION_PLACEHOLDER)
                    .doesNotContain("null");
            assertThat(RealtimeRecommendType.DEFAULT.pickTitle(spaceId, "   "))
                    .doesNotContain(RealtimeRecommendType.REGION_PLACEHOLDER);
        }
    }

    @Test
    @DisplayName("같은 공간은 항상 같은 문구가 선택된다")
    void pickTitle_isDeterministic() {
        String firstTitle = RealtimeRecommendType.NEW.pickTitle(42L, "성수동");
        String firstSubtitle = RealtimeRecommendType.NEW.pickSubtitle(42L);

        assertThat(RealtimeRecommendType.NEW.pickTitle(42L, "성수동")).isEqualTo(firstTitle);
        assertThat(RealtimeRecommendType.NEW.pickSubtitle(42L)).isEqualTo(firstSubtitle);
    }

    @Test
    @DisplayName("모든 유형이 타이틀과 서브텍스트를 갖는다")
    void allTypesHaveMents() {
        for (RealtimeRecommendType type : RealtimeRecommendType.values()) {
            assertThat(type.getTitles()).isNotEmpty();
            assertThat(type.getSubtitles()).isNotEmpty();
            assertThat(type.pickTitle(1L, "성수동")).isNotBlank();
            assertThat(type.pickSubtitle(1L)).isNotBlank();
        }
    }
}