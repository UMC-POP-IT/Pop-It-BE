package com.popIt.pop_it.domain.identity_verification.entity;

import com.popIt.pop_it.domain.identity_verification.entity.enums.Gender;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.global.util.CryptoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;

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
    @Column(nullable = false, unique = true)
    private String identityVerificationId;

    // 암호화
    @Convert(converter = CryptoConverter.class)
    @Column(nullable = false)
    private String name;

    // 암호화
    @Convert(converter = CryptoConverter.class)
    @Column(nullable = false)
    private String birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    // 암호화
    @Convert(converter = CryptoConverter.class)
    @Column(nullable = false)
    private String phone;

    // 암호화
    // 실제 값은 암호화, 필요할 때만 키로 복호화
    @Convert(converter = CryptoConverter.class)
    @Column(nullable = false)
    private String ci;

    // ci값 SHA-256 해시(원본 값 → 해시값으로 단방향 변환), 조회/중복체크용이므로 unique 필수
    @Column(nullable = false, unique = true)
    private String ciHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime verifiedAt;

    // 인증 상태
    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, unique = true)
    private User user;
}
