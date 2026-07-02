package com.popIt.pop_it.domain.facility.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityName {

    // 냉난방
    INDIVIDUAL_HEATING("개별 난방"),
    CENTRAL_HEATING("중앙 난방"),
    DISTRICT_HEATING("지역 난방"),
    WALL_MOUNTED_AC("벽걸이 에어컨"),
    STAND_AC("스탠드 에어컨"),
    CEILING_AC("천장 에어컨"),

    // 보안
    ENTRANCE_SECURITY("현관 보안"),
    CCTV("CCTV"),
    SECURITY_WINDOW("방범창"),
    KEY_CARD("카드키"),
    IN_HOUSE_SECURITY_GUARD("자체 경비원"),
    PRIVATE_SECURITY("사설 경비"),

    // 기타
    FIRE_ALARM("화재 경보기"),
    FIRE_EXTINGUISHER("소화기"),
    WIFI("WIFI"),
    RESTROOM("화장실")
    ;

    private final String description;
}
