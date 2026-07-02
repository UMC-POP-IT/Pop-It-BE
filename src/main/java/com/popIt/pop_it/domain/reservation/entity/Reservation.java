package com.popIt.pop_it.domain.reservation.entity;

import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.space.entity.Space;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 예약 식별자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING_APPROVAL; // 예약 상태 (기본값: 승인대기)

    @Column(nullable = false)
    private LocalDate startDate; // 이용 시작일

    @Column(nullable = false)
    private LocalDate endDate; // 이용 종료일

    @Column(length = 200, nullable = false)
    private String usagePurpose; // 이용 목적

    @Column(nullable = false)
    private Long rentalFee; // 대여료

    @Column(nullable = false)
    private Long deposit; // 보증금

    @Column(nullable = false)
    private Long insuranceFee; // 보험료

    @Column(nullable = false)
    private Long totalPrice; // 총 결제 금액

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Space space; // 예약 대상 공간

    // @TODO: User 엔티티가 만들어지면 @ManyToOne 관계로 변경 필요
    @Column(nullable = false)
    private Long userId; // 게스트 ID
}
