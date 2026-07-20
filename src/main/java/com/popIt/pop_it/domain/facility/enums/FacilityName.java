package com.popIt.pop_it.domain.facility.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityName {

    // 냉난방
    INDIVIDUAL_HEATING(FacilityCategory.HEATING_COOLING, "개별 난방"),
    CENTRAL_HEATING(FacilityCategory.HEATING_COOLING, "중앙 난방"),
    DISTRICT_HEATING(FacilityCategory.HEATING_COOLING, "지역 난방"),
    WALL_MOUNTED_AC(FacilityCategory.HEATING_COOLING, "벽걸이 에어컨"),
    STAND_AC(FacilityCategory.HEATING_COOLING, "스탠드 에어컨"),
    CEILING_AC(FacilityCategory.HEATING_COOLING, "천장 에어컨"),

    // 보안
    ENTRANCE_SECURITY(FacilityCategory.SECURITY, "현관 보안"),
    CCTV(FacilityCategory.SECURITY, "CCTV"),
    SECURITY_WINDOW(FacilityCategory.SECURITY, "방범창"),
    KEY_CARD(FacilityCategory.SECURITY, "카드키"),
    IN_HOUSE_SECURITY_GUARD(FacilityCategory.SECURITY, "자체 경비원"),
    PRIVATE_SECURITY(FacilityCategory.SECURITY, "사설 경비"),

    // 기타
    FIRE_ALARM(FacilityCategory.ETC, "화재 경보기"),
    FIRE_EXTINGUISHER(FacilityCategory.ETC, "소화기"),
    WIFI(FacilityCategory.ETC, "WIFI"),
    RESTROOM(FacilityCategory.ETC, "화장실")
    ;

    private final FacilityCategory category;
    private final String description;
}
