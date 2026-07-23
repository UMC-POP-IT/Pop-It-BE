package com.popIt.pop_it.domain.reservation.repository;

import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 게스트 "내 예약 내역" 목록 - 커서 기반 (createdAt:id 복합 커서), 상태 필터 선택
    // space/user는 *-to-one 관계라 join fetch로 페이징과 같이 써도 안전 (N+1 방지)
    @Query("""
        select r from Reservation r
        join fetch r.space
        join fetch r.user
        where r.user.userId = :userId
        and (:status is null or r.status = :status)
        and (
            :cursorCreatedAt is null
            or r.createdAt < :cursorCreatedAt
            or (r.createdAt = :cursorCreatedAt and r.id < :cursorId)
        )
        order by r.createdAt desc, r.id desc
        """)
    Slice<Reservation> findMyReservationsByCursor(
            @Param("userId") Long userId,
            @Param("status") ReservationStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 호스트 "예약관리" 목록 - 커서 기반, 상태 필터 선택
    @Query("""
        select r from Reservation r
        join fetch r.space
        join fetch r.user
        where r.space.hostId = :hostId
        and (:status is null or r.status = :status)
        and (
            :cursorCreatedAt is null
            or r.createdAt < :cursorCreatedAt
            or (r.createdAt = :cursorCreatedAt and r.id < :cursorId)
        )
        order by r.createdAt desc, r.id desc
        """)
    Slice<Reservation> findHostReservationsByCursor(
            @Param("hostId") Long hostId,
            @Param("status") ReservationStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 게스트 상태별 탭 카운트
    @Query("""
        select r.status as status, count(r) as count
        from Reservation r where r.user.userId = :userId group by r.status
        """)
    List<ReservationStatusCount> countMyReservationsByStatus(@Param("userId") Long userId);

    // 호스트 상태별 탭 카운트
    @Query("""
        select r.status as status, count(r) as count
        from Reservation r where r.space.hostId = :hostId group by r.status
        """)
    List<ReservationStatusCount> countHostReservationsByStatus(@Param("hostId") Long hostId);

    // 동시 예약 방지용 - 특정 공간의 특정 기간에 겹치는 활성 예약 존재 여부 확인 (Space 락으로 보호됨)
    @Query("""
        select count(r) > 0 from Reservation r
        where r.space.id = :spaceId
        and r.status not in :excludedStatuses
        and r.startDate <= :endDate and r.endDate >= :startDate
        """)
    boolean existsOverlappingReservation(
            @Param("spaceId") Long spaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            // excludedStatuses는 CANCELLED 같은 상태를 겹침 체크에서 빼기 위한 파라미터 (호출부에서 List.of(CANCELLED) 넘기면 됨)
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses
    );

    // 공간별 예약 불가(선점된) 기간 목록 - 캘린더 비활성화 표시용 (지난 예약은 제외)
    @Query("""
        select r.startDate as startDate, r.endDate as endDate
        from Reservation r
        where r.space.id = :spaceId
        and r.status not in :excludedStatuses
        and r.endDate >= :today
        order by r.startDate
        """)
    List<ReservationDateRange> findUnavailableDateRangesBySpaceId(
            @Param("spaceId") Long spaceId,
            @Param("excludedStatuses") List<ReservationStatus> excludedStatuses,
            @Param("today") LocalDate today
    );

    List<Reservation> findAllByStatusAndStartDateLessThanEqual(ReservationStatus status, LocalDate date);
    List<Reservation> findAllByStatusAndEndDateBefore(ReservationStatus status, LocalDate date);
    List<Reservation> findAllByStatusAndCheckoutRejectedFalseAndCheckoutSubmittedAtBefore(ReservationStatus status, LocalDateTime cutoff);
    List<Reservation> findAllByStatusAndCheckoutRejectedFalseAndCheckoutSubmittedAtIsNullAndEndDateLessThanEqual(ReservationStatus status, LocalDate cutoffDate);
    List<Reservation> findAllByStatusAndCheckoutRejectedTrueAndCheckoutRejectedAtBefore(ReservationStatus status, LocalDateTime cutoff);
}
