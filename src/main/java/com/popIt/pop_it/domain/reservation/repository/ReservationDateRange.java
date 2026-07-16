package com.popIt.pop_it.domain.reservation.repository;

import java.time.LocalDate;

// 공간별 예약 불가(선점된) 기간 조회용 프로젝션
public interface ReservationDateRange {
    LocalDate getStartDate();
    LocalDate getEndDate();
}
