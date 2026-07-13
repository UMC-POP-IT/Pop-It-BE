package com.popIt.pop_it.domain.payment.entity;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.payment.enums.PaymentMethod;
import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
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
}
