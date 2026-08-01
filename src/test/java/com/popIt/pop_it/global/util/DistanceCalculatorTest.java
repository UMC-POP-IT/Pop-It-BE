package com.popIt.pop_it.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class DistanceCalculatorTest {

    @Test
    void 같은_좌표는_거리가_0이다() {
        double distance = DistanceCalculator.haversineKm(37.5665, 126.9780, 37.5665, 126.9780);

        assertThat(distance).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void 적도에서_경도_1도_차이는_약_111km다() {
        double distance = DistanceCalculator.haversineKm(0.0, 0.0, 0.0, 1.0);

        assertThat(distance).isCloseTo(111.19, within(0.5));
    }

    @Test
    void 서울시청과_강남역_거리는_약_8km다() {
        double distance = DistanceCalculator.haversineKm(37.5665, 126.9780, 37.4979, 127.0276);

        assertThat(distance).isCloseTo(8.4, within(1.0));
    }
}
