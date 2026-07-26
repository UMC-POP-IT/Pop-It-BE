package com.popIt.pop_it.domain.user_activity.entity;

import com.popIt.pop_it.domain.user_activity.entity.enums.ActivityType;
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
@Table(name = "user_activity")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userActivityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    public void recordView() {
        this.viewCount += 1;
        this.lastViewedAt = LocalDateTime.now();
    }
}
