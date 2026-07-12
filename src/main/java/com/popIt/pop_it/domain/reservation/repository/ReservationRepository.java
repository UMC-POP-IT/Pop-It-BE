package com.popIt.pop_it.domain.reservation.repository;

import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 게스트 "내 예약 내역" 목록
    List<Reservation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 호스트 "예약관리" 목록
    @Query("select r from Reservation r where r.space.hostId = :hostId order by r.createdAt desc")
    List<Reservation> findAllByHostId(@Param("hostId") Long hostId);

    // 상세 조회 시 space, user 함께 로딩 (지연로딩 N+1 방지)
    @Query("select r from Reservation r join fetch r.space join fetch r.user where r.id = :id")
    Optional<Reservation> findByIdWithDetails(@Param("id") Long id);

    // 동시 예약 방지용 - 특정 공간의 특정 기간에 겹치는 활성 예약 존재 여부 확인 (락은 추후 구현)
    @Query("""
        select count(r) > 0 from Reservation r
        where r.space.id = :spaceId
        and r.status not in :excludedStatuses
        and r.startDate <= :endDate and r.endDate >= :startDate
        """)
    boolean existsOverlappingReservation(
            @Param("spaceId") Long spaceId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            // excludedStatuses는 CANCELLED 같은 상태를 겹침 체크에서 빼기 위한 파라미터 (호출부에서 List.of(CANCELLED) 넘기면 됨)
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses
    );
}
