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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Reservation reservation; // 대상 예약
}
