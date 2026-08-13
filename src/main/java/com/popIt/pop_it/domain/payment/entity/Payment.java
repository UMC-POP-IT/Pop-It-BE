package com.popIt.pop_it.domain.payment.entity;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.enums.SettlementStepStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 결제 식별자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status; // 결제 상태

    @Column(nullable = false, unique = true, length = 64)
    private String orderId; // 토스에 전달하는 주문번호 (prepare 시점 생성)

    @Column(unique = true, length = 200)
    private String paymentKey; // 토스 결제 고유 키 (confirm 성공 후 저장)

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentMethod method; // 결제 수단 (confirm 성공 후 저장)

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey; // 중복 주문 생성 방지 키

    private LocalDateTime paidAt; // 결제 완료 일시

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일시

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract; // 대상 계약

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SettlementStepStatus hostPayoutStatus = SettlementStepStatus.PENDING; // 호스트 임대료 지급 상태

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SettlementStepStatus depositRefundStatus = SettlementStepStatus.PENDING; // 게스트 보증금 환불 상태

    private LocalDateTime hostPayoutAt; // 호스트 지급 완료 일시

    private LocalDateTime depositRefundedAt; // 보증금 환불 완료 일시

    // 토스 결제 승인 성공 처리
    public void markAsPaid(String paymentKey, PaymentMethod method, LocalDateTime paidAt) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.paidAt = paidAt;
        this.status = PaymentStatus.PAID;
    }

    // 호스트 지급, 보증금 환불은 각각 별도의 외부 API 호출이라 독립적으로 성공/실패할 수 있어
    // 두 단계를 따로 추적하고, 실패한 쪽만 재시도할 수 있게 한다.
    public void markHostPayoutDone(LocalDateTime hostPayoutAt) {
        this.hostPayoutStatus = SettlementStepStatus.DONE;
        this.hostPayoutAt = hostPayoutAt;
    }

    public void markHostPayoutFailed() {
        this.hostPayoutStatus = SettlementStepStatus.FAILED;
    }

    public void markDepositRefundDone(LocalDateTime depositRefundedAt) {
        this.depositRefundStatus = SettlementStepStatus.DONE;
        this.depositRefundedAt = depositRefundedAt;
    }

    public void markDepositRefundFailed() {
        this.depositRefundStatus = SettlementStepStatus.FAILED;
    }
}
