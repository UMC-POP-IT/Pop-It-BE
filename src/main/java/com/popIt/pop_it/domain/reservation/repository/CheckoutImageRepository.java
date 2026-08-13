package com.popIt.pop_it.domain.reservation.repository;

import com.popIt.pop_it.domain.reservation.entity.CheckoutImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckoutImageRepository extends JpaRepository<CheckoutImage, Long> {

    // reservationId 목록 중 현재 유효한(isActive=true) 퇴실 사진이 1장 이상 제출된 예약 ID만 배치로 조회 (isPhotoVerified 판단용)
    @Query("""
        select distinct ci.reservation.id from CheckoutImage ci
        where ci.reservation.id in :reservationIds and ci.isActive = true
        """)
    List<Long> findVerifiedReservationIds(@Param("reservationIds") List<Long> reservationIds);

    // 현재 유효한(거절되지 않은) 제출 사진만 조회 - 호스트 승인/거절 전 확인용
    List<CheckoutImage> findAllByReservationIdAndIsActiveTrueOrderBySortOrder(Long reservationId);

    // 가장 최근 거절 배치의 사진만 조회 - 게스트가 거절된 제출 확인용 (여러 번 거절된 경우 이전 배치는 제외)
    @Query("""
        select ci from CheckoutImage ci
        where ci.reservation.id = :reservationId
          and ci.isActive = false
          and ci.rejectedAt = (
              select max(ci2.rejectedAt) from CheckoutImage ci2
              where ci2.reservation.id = :reservationId and ci2.isActive = false
          )
        order by ci.sortOrder
        """)
    List<CheckoutImage> findLatestRejectedByReservationId(@Param("reservationId") Long reservationId);

    // 퇴실 거절 시 기존 제출 사진을 하드 삭제하지 않고 비활성화 + 거절 시각 기록 (이력 보존)
    @Modifying
    @Query("update CheckoutImage ci set ci.isActive = false, ci.rejectedAt = :rejectedAt where ci.reservation.id = :reservationId and ci.isActive = true")
    void deactivateAllByReservationId(@Param("reservationId") Long reservationId, @Param("rejectedAt") LocalDateTime rejectedAt);

}
