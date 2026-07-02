package com.popIt.pop_it.domain.escrow.entity;

import com.popIt.pop_it.domain.escrow.enums.EscrowStatus;
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
@Table(name = "escrow")
public class Escrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 에스크로 식별자

    @Column(nullable = false)
    private String escrowTransactionId; // 에스크로사 발급 거래 ID

    @Column(nullable = false)
    private Long amount; // 예치 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus status; // 에스크로 상태

    private LocalDateTime depositedAt; // 입금 일시

    private LocalDateTime releasedAt; // 정산 일시

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Reservation reservation; // 대상 예약
}
