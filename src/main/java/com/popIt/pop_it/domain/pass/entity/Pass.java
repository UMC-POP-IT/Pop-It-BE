package com.popIt.pop_it.domain.pass.entity;

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
@Table(name = "pass")
public class Pass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Telecom telecom;

    // 암호화
    @Column(nullable = false)
    private String ci;

    // 암호화
    @Column(nullable = false)
    private String di;

    @Column(nullable = false, updatable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
}
