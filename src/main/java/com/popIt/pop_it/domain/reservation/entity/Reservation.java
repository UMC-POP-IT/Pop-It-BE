package com.popIt.pop_it.domain.reservation.entity;

import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.exception.ReservationException;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    private Long platformFee; // 플랫폼 수수료

    @Column(nullable = false)
    private Long totalPrice; // 총 결제 금액

    @Column
    private LocalDateTime checkoutSubmittedAt; // 퇴실 증빙 제출 시각 (자동승인 기준)

    @Column(nullable = false)
    @Builder.Default
    // 호스트가 퇴실 증빙을 거절한 상태(재인증 대기)인지 여부
    private Boolean checkoutRejected = false; // 호스트가 퇴실 거부한 경우 스케줄러가 작동 안하도록

    @Column
    private LocalDateTime checkoutRejectedAt; // 퇴실 거절 시각 (거절 후 재제출 없을 시 자동승인 기준)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Space space; // 예약 대상 공간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user; // 게스트

    //도메인 메서드

    //예약 승인
    public void approve() {
        this.status = ReservationStatus.APPROVED;
    }

    //예약 거절
    public void reject() {
        this.status = ReservationStatus.CANCELLED;
    }

    //예약 취소(호출부에서 구분되도록 reject()와 분리)
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    //사용 중 -> 이용 완료
    public void completeUsage() {
        this.status = ReservationStatus.USAGE_COMPLETED;
    }

    //계약완료 -> 사용 중 (이용 시작일 도래)
    public void startUsage() {
        this.status = ReservationStatus.IN_USE;
    }

    //계약 완료 기록
    public void markContractCompleted() {
        if (this.status != ReservationStatus.APPROVED) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
        }
        this.status = ReservationStatus.CONTRACT_COMPLETED;
    }

    //퇴실 증빙 제출 시간 기록
    public void markCheckoutSubmitted() {
        this.checkoutSubmittedAt = LocalDateTime.now();
        this.checkoutRejected = false;
    }

    //퇴실 증빙 거절(재인증 대기 상태로 전환)
    public void rejectCheckout() {
        this.checkoutRejected = true;
        this.checkoutRejectedAt = LocalDateTime.now();
    }

    //퇴실 완료
    public void completeCheckout() {
        this.status = ReservationStatus.CHECKOUT_COMPLETED;
    }

    // 기간 계산 (시작일/종료일 모두 포함)
    public long getPeriod() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    // 호스트의 총 금액
    public Long getHostTotalPrice() {
        return rentalFee - platformFee;
    }
}
