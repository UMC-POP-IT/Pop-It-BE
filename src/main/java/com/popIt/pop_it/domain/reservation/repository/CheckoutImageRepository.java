package com.popIt.pop_it.domain.reservation.repository;

import com.popIt.pop_it.domain.reservation.entity.CheckoutImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CheckoutImageRepository extends JpaRepository<CheckoutImage, Long> {

    // reservationId 목록 중 퇴실 사진이 1장 이상 제출된 예약 ID만 배치로 조회 (isPhotoVerified 판단용)
    @Query("""
        select distinct ci.reservation.id from CheckoutImage ci
        where ci.reservation.id in :reservationIds
        """)
    List<Long> findVerifiedReservationIds(@Param("reservationIds") List<Long> reservationIds);

    List<CheckoutImage> findAllByReservationIdOrderBySortOrder(Long reservationId);

    // 퇴실 거절 시 기존 제출 사진 초기화용
    void deleteAllByReservationId(Long reservationId);

}
