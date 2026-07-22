package com.popIt.pop_it.domain.scene.entity;

import com.popIt.pop_it.domain.space.entity.Space;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "scene")
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // 씬(방) 이름

    @Column(nullable = false, length = 500)
    private String modelUrl; // 3D 모델(.glb) URL

    @Column(length = 500)
    private String thumbnail; // 썸네일 이미지 URL

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false; // 공간의 기본 씬 여부

    // 카메라 초기 설정
    @Column(nullable = false)
    private Double cameraPositionX;

    @Column(nullable = false)
    private Double cameraPositionY;

    @Column(nullable = false)
    private Double cameraPositionZ;

    @Column(nullable = false)
    private Double cameraTargetX;

    @Column(nullable = false)
    private Double cameraTargetY;

    @Column(nullable = false)
    private Double cameraTargetZ;

    @Column(nullable = false)
    private Double minDistance;

    @Column(nullable = false)
    private Double maxDistance;

    //연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Space space;

    //타임스탬프
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt; // soft delete

    //부분 수정 - null인 필드는 기존 값 유지
    public void update(String name, String modelUrl, String thumbnail) {
        if (name != null) this.name = name;
        if (modelUrl != null) this.modelUrl = modelUrl;
        if (thumbnail != null) this.thumbnail = thumbnail;
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }
}
