package com.popIt.pop_it.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "checkout_image")
public class CheckoutImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkout_image_url", nullable = false, length = 255)
    private String checkoutImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // 현재 유효한 제출 건인지 여부 - 호스트 거절 시 하드 삭제 대신 false로 전환해 이력을 보존
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 비활성화(거절)된 시각 - 여러 번 거절된 경우 가장 최근 거절 배치를 구분하는 용도
    @Column
    private LocalDateTime rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    //퇴실 거절 시 비활성화(soft delete)
    public void deactivate(LocalDateTime rejectedAt) {
        this.isActive = false;
        this.rejectedAt = rejectedAt;
    }
}

