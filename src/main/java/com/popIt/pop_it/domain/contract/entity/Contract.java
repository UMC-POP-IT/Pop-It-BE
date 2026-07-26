package com.popIt.pop_it.domain.contract.entity;

import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
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
@Table(name = "contract")
public class Contract {

    @Version
    @Column(nullable = false)
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 계약서 식별자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ContractStatus status = ContractStatus.HOST_SIGNATURE_PENDING; // 계약 상태 (기본값: 호스트서명대기)

    @Column(length = 255)
    private String hostSignatureUrl; // 호스트 서명 URL

    @Column(length = 255)
    private String guestSignatureUrl; // 게스트 서명 URL

    private LocalDateTime hostSignedAt; // 호스트 서명 일시

    private LocalDateTime guestSignedAt; // 게스트 서명 일시

    /**
     * Reservation의 스냅샷 (계약 체결 시점 확정 정보)---------------
     */
    @Column(nullable = false)
    private Long hostId;

    @Column(nullable = false)
    private Long guestId;

    @Column(nullable = false)
    private Long spaceId;


    @Column(nullable = false)
    private LocalDate startDate; // 이용 시작일

    @Column(nullable = false)
    private LocalDate endDate; // 이용 종료일

    @Column(length = 200, nullable = false)
    private String usagePurpose; // 사용 목적

    @Column(nullable = false)
    private Long rentalFee; // 임대료

    @Column(nullable = false)
    private Long deposit; // 보증금

    @Column(nullable = false)
    private Long insuranceFee; // 보험료

    @Column(nullable = false)
    private Long platformFee; // 플랫폼 수수료; 호스트-게스트 계약서에 쓰이진 않지만 계약 당시 값을 기록해두기 위함

    @Column(nullable = false)
    private Long totalPrice; // 총 결제 금액
    /**
     * ----------------------------------
     */

    /**
     * 계약 위변조 검증용 Hash --------------
     */
    private String hostSignerCiHash; // 호스트 본인인증 ci Hash
    private String guestSignerCiHash; // 게스트 본인인증 ci Hash
    private String hostSignatureImgHash; // 호스트 서명 이미지 Hash
    private String guestSignatureImgHash; // 게스트 서명 이미지 Hash

    // 예약 확정 -> 계약 생성 시점 즉시 확성되는 값. 서명 처리 함수 등에서 재계산 금지
    @Column(nullable = false)
    private String contentHash; // 계약 내용(이용 목적, 이용 시작일, 종료일, 금액 등) Hash
    /**
     * ----------------------------------
     */

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Reservation reservation; // 대상 예약

    /**
     * 도메인 메서드
     */
    // 호스트 서명: 호스트 서명 정보 업데이트
    public void signByHost(ContractStatus status, String hostSignatureUrl, String hostSignerCiHash, String hostSignatureImgHash) {
        this.status = status;
        this.hostSignatureUrl = hostSignatureUrl;
        this.hostSignedAt = LocalDateTime.now();
        this.hostSignerCiHash = hostSignerCiHash;
        this.hostSignatureImgHash = hostSignatureImgHash;
    }
    // 게스트 서명: 게스트 서명 정보 업데이트
    public void signByGuest(ContractStatus status, String guestSignatureUrl, String guestSignerCiHash, String guestSignatureImgHash) {
        this.status = status;
        this.guestSignatureUrl = guestSignatureUrl;
        this.guestSignedAt = LocalDateTime.now();
        this.guestSignerCiHash = guestSignerCiHash;
        this.guestSignatureImgHash = guestSignatureImgHash;
    }

    public void markAsCompleted() {
        this.status = ContractStatus.COMPLETED;
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
