package com.popIt.pop_it.domain.user.entity;

import com.popIt.pop_it.domain.user.entity.enums.Bank;
import com.popIt.pop_it.domain.user.entity.enums.TaxationType;
import com.popIt.pop_it.global.util.CryptoConverter;
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
@Table(name = "host_profile")
public class HostProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxationType taxationType;

    // 사업자등록번호: 민감정보 → 암호화 저장 (평문 세팅, 암복호화는 CryptoConverter 위임)
    @Convert(converter = CryptoConverter.class)
    @Column(nullable = false)
    private String businessRegistrationNumber;

    @Column(nullable = false)
    private String businessLicenseUrl;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessAddress;

    @Column(nullable = false)
    private String bankbookCopyUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Bank bank;

    // 정산 계좌번호: 금융 민감정보 → 암호화 저장
    @Convert(converter = CryptoConverter.class)
    @Column(nullable = false)
    private String settlementAccountNumber;

    // 예금주(개인정보) → 암호화 저장 (암호문 길이 고려로 length 제한 제거)
    @Convert(converter = CryptoConverter.class)
    @Column(nullable = false)
    private String accountHolder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 한 사용자당 호스트 프로필 1개 → DB 유니크 제약으로 동시성(TOCTOU) 중복 방지
    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private Long userId;
}
