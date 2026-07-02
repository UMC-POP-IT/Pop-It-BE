package com.popIt.pop_it.domain.user.entity;

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

    @Column(nullable = false)
    private String settlementAccountNumber;

    @Column(length = 20, nullable = false)
    private String accountHolder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
}
