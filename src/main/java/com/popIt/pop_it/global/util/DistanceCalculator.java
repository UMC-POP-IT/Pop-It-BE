package com.popIt.pop_it.global.util;

/**
 * 두 좌표 간 직선거리(km)를 Haversine 공식으로 계산하는 유틸.
 * 별도의 지역 인접 관계 테이블 없이, 좌표 기반으로 "가까운 지역/공간"을 판단할 때 쓴다.
 */
public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {}

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
