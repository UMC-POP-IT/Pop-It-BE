package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 실시간 추천의 가동률 판정에 필요한 결제 지표만 읽는 레포지토리
public interface SpaceUtilizationRepository extends JpaRepository<Payment, Long> {
    // 공간별 결제 완료 건수
    @Query("""
            select s.id as spaceId, count(p) as paidCount
            from Payment p
                join p.contract c
                join c.reservation r
                join r.space s
            where p.status = :paidStatus
                and s.id in :spaceIds
                and p.paidAt >= :since
            group by s.id
            """)
    List<SpacePaidCount> countPaidBySpaceIds(
            @Param("paidStatus")PaymentStatus paidStatus,
            @Param("spaceIds") List<Long> spaceIds,
            @Param("since") LocalDateTime since
    );

    // 결제 완료된 예약의 이용 기간
    @Query("""
            select s.id as spaceId, r.startDate as startDate, r.endDate as endDate
            from Payment p
                join p.contract c
                join c.reservation r
                join r.space s
            where p.status = :paidStatus
            and r.status <> :cancelledStatus
            and s.id in :spaceIds
            and r.startDate <= :periodEnd
            and r.endDate >= :periodStart
            """)
    List<SpacePaidPeriod> findPaidPeriodsBySpaceIds(
            @Param("paidStatus") PaymentStatus paidStatus,
            @Param("cancelledStatus") ReservationStatus cancelledStatus,
            @Param("spaceIds") List<Long> spaceIds,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}