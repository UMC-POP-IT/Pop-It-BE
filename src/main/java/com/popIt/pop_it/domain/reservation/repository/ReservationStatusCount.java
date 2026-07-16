package com.popIt.pop_it.domain.reservation.repository;

import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;

// 상태별 예약 개수 집계용 프로젝션 (탭 배지 카운트)
public interface ReservationStatusCount {
    ReservationStatus getStatus();
    Long getCount();
}
