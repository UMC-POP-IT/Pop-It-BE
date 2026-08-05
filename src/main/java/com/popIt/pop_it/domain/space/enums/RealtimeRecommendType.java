package com.popIt.pop_it.domain.space.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum RealtimeRecommendType {
    // 1. 가동률 하위 공간 우선 노출
    LOW_UTILIZATION(
            List.of(
                    "이번 주, 당장 시작하는\n팝업 공간",
                    "원하는 날짜에 여유롭게\n추천 아지트",
                    "아직 발견되지 않은\n동네 숨은 핫플"
            ),
            List.of(
                    "합리적인 예산으로 바로 예약 가능한\n실속 있는 공간들이에요.",
                    "북적이는 거리에서 한 블록 뒤에 있는\n매력적인 유휴 공간이에요.",
                    "일정 조율 스트레스 없이 첫 팝업을\n바로 준비해 보세요."
            )
    ),

    // 2. 콜드 스타트 지원 (신규 등록 공간)
    NEW(
            List.of(
                    "가장 먼저 예약하는\n첫 팝업의 기회",
                    "팝잇에 새로 찾아온\n따끈한 신규 공간",
                    "아무도 쓰지 않은 공간\n빈 도화지처럼"
            ),
            List.of(
                    "깨끗하게 정돈된 신규 공간에서\n첫 리뷰의 주인공이 되어요.",
                    "새로운 영감을 줄 매력적인 공간을\n가장 먼저 소개합니다.",
                    "이제 막 등록된 공간에\n브랜드를 입혀보세요."
            )
    ),

    // 3. 기본 공간형 (Default)
    DEFAULT(
            List.of(
                    "잠들어 있던 공간,\n새로운 가치를",
                    "당신의 기획이 현실이 되는\n무한한 공간",
                    "어떤 아이디어든 스며드는\n베이직한 공간",
                    "성공적인 팝업의 시작\n[지역] 공간",
                    "당신의 브랜드를 핫하게 보여줄\n[지역] 핫플"
            ),
            List.of(
                    "비어있는 공간에 당신의 다채로운\n브랜드를 불어넣어 보세요.",
                    "팝잇과 함께 유휴 공간의 새로운\n가능성을 발견해 보세요.",
                    "브랜드 인지도 상승을 위한\n최적의 공간이에요."
            )
    );

    // 타이틀 문구 안에서 실제 지역으로 치환할 자리표시자
    public static final String REGION_PLACEHOLDER = "[지역]";

    // 신규 공간 판정 기준
    private static final int NEW_THRESHOLD_DAYS = 30;

    private final List<String> titles;
    private final List<String> subtitles;

    // 공간 유형 판정
    public static RealtimeRecommendType classify(
            LocalDateTime createdAt,
            LocalDateTime now,
            boolean lowUtilization
    ) {
        if (isNew(createdAt, now)) {
            return NEW;
        }

        if (lowUtilization) {
            return LOW_UTILIZATION;
        }

        return DEFAULT;
    }

    // 등록 30일 이내면 신규 공간
    private static boolean isNew(LocalDateTime createdAt, LocalDateTime now) {
        return createdAt != null && createdAt.isAfter(now.minusDays(NEW_THRESHOLD_DAYS));
    }

    // 캐러셀 앞단에 우선 배치할 추천 유형인지 여부
    public boolean isFrontSlotType() {
        return this == LOW_UTILIZATION || this == NEW;
    }

    // 카드 타이틀 고르기
    // 지역명을 구하지 못한 공간에는 [지역]이 들어간 문구를 후보에서 제외
    public String pickTitle(long spaceId, String region) {
        boolean hasRegion = region != null && !region.isBlank();

        List<String> candidates = hasRegion
                ? titles
                : titles.stream()
                    .filter(title -> !title.contains(REGION_PLACEHOLDER))
                    .toList();

        String picked = candidates.get((int) Math.floorMod(spaceId, candidates.size()));

        return hasRegion ? picked.replace(REGION_PLACEHOLDER, region) : picked;
    }

    // 카드마다 문구가 반복되지 않도록 spaceId로 분산 선택
    public String pickSubtitle(long spaceId) {
        return subtitles.get((int) Math.floorMod(spaceId, subtitles.size()));
    }
}
