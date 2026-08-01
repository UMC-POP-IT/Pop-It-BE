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
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_social_provider_social_uid",
                columnNames = {"social_provider", "social_uid"}
        )
)
public class User {

    public static final int MAX_NICKNAME_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SocialProvider socialProvider;

    @Column(nullable = false, updatable = false)
    private String socialUid;

    @Column(length = MAX_NICKNAME_LENGTH, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserMode currentMode;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 1024)
    private String refreshToken;

    private LocalDateTime deletedAt;

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    public void switchToHost() {
        this.currentMode = UserMode.HOST;
    }

    public void switchToGuest() {
        this.currentMode = UserMode.GUEST;
    }
}
