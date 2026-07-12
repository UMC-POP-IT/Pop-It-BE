package com.popIt.pop_it.domain.identity_verification.entity;

import com.popIt.pop_it.domain.identity_verification.entity.enums.Gender;
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
@Table(name = "identity_verification")
public class IdentityVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 인증 시도 식별자
    @Column(nullable = false)
    private String identityVerificationId;

    // 암호화
    @Column(nullable = false)
    private String name;

    // 암호화
    @Column(nullable = false)
    private String birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // 암호화
    @Column(nullable = false)
    private String phone;

    // 암호화
    @Column(nullable = false)
    private String ci;

    @Column(nullable = false, updatable = false)
    private LocalDateTime verifiedAt;

    // 인증 상태
    @Column(nullable = false)
    private String status;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
}
