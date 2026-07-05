package com.popIt.pop_it.domain.user.entity;

import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SocialProvider socialProvider;

    @Column(nullable = false, updatable = false)
    private String socialUid;

    @Column(length = 30, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserMode currentMode;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
