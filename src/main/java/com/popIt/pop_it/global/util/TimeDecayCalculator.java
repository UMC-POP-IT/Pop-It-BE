package com.popIt.pop_it.global.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 유저 취향 벡터 계산 시 최근 행동일수록 더 크게 반영하기 위한 지수 감쇠 유틸.
 */
public final class TimeDecayCalculator {

    private static final double DEFAULT_HALF_LIFE_DAYS = 3.0;

    private TimeDecayCalculator() {}

    public static double decay(LocalDateTime eventTime, LocalDateTime now) {
        return decay(eventTime, now, DEFAULT_HALF_LIFE_DAYS);
    }

    public static double decay(LocalDateTime eventTime, LocalDateTime now, double halfLifeDays) {
        double daysElapsed = Duration.between(eventTime, now).toSeconds() / 86400.0;
        double lambda = Math.log(2) / halfLifeDays;
        return Math.exp(-lambda * Math.max(daysElapsed, 0));
    }

    public static double actionWeight(EngagementType type) {
        return switch (type) {
            case WISHLIST -> 2.0;
            case VIEW -> 1.0;
        };
    }
}
