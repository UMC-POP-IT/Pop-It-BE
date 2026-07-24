package com.popIt.pop_it.domain.user_agreement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "user_agreement",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_term",
        columnNames = {"user_id", "term_id"})
)
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean isAgreed;

    @Column(nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "term_id", nullable = false, updatable = false)
    private Long termId;

    // 이미 동의 이력이 있는 약관을 다시 요청받았을 때 동의 여부/시각을 갱신 (upsert의 update 경로)
    public void updateAgreement(boolean isAgreed, LocalDateTime agreedAt) {
        this.isAgreed = isAgreed;
        this.agreedAt = agreedAt;
    }
}
