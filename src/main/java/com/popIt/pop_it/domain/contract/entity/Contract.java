package com.popIt.pop_it.domain.contract.entity;

import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import jakarta.persistence.*;
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
@Table(name = "contract")
public class Contract {

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
     * 전자서명 시 필요한 해시값
     */
    private String hostSignerCiHash; // 호스트 본인인증 ci 해시
    private String guestSignerCiHash; // 게스트 본인인증 ci 해시
    private String hostSignatureImgHash; // 호스트 서명 이미지 위변조 검증
    private String guestSignatureImgHash; // 게스트 서명 이미지 위변조 검증

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(nullable = false)
    private Long rentalFee; // 임대료 (계약 체결 시점 확정 금액)

    @Column(nullable = false)
    private Long deposit; // 보증금 (계약 체결 시점 확정 금액)

    @Column(nullable = false)
    private Long insuranceFee; // 보험료 (계약 체결 시점 확정 금액)

    @Column(nullable = false)
    private Long totalPrice; // 총 결제 금액 (계약 체결 시점 확정 금액)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Reservation reservation; // 대상 예약

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
}
