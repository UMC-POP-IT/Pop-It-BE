package com.popIt.pop_it.domain.wishlist.entity;

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
    name = "wishlist",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_space",
        columnNames = {"user_id", "space_id"})
)
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "space_id", nullable = false, updatable = false)
    private Long spaceId;
}
