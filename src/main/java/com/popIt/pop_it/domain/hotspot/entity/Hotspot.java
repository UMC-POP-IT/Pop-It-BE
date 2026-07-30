package com.popIt.pop_it.domain.hotspot.entity;

import com.popIt.pop_it.domain.hotspot.enums.HotspotType;
import com.popIt.pop_it.domain.scene.entity.Scene;
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
@Table(name = "hotspot")
public class Hotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double positionX;

    @Column(nullable = false)
    private Double positionY;

    @Column(nullable = false)
    private Double positionZ;

    @Column(nullable = false, length = 50)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HotspotType type;

    @Column(length = 500)
    private String description; // INFO 타입일 때만 사용

    private Long targetSceneId; // LINK 타입일 때만 사용 (연관관계 대신 단순 참조 - 자기참조 캐스케이드 복잡도 회피)

    //연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Scene scene;

    //타임스탬프
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //부분 수정 - null인 필드는 기존 값 유지 (type은 생성 후 변경 불가)
    public void update(Double positionX, Double positionY, Double positionZ, String label,
                        String description, Long targetSceneId) {
        if (positionX != null) this.positionX = positionX;
        if (positionY != null) this.positionY = positionY;
        if (positionZ != null) this.positionZ = positionZ;
        if (label != null) this.label = label;
        if (description != null) this.description = description;
        if (targetSceneId != null) this.targetSceneId = targetSceneId;
    }
}
