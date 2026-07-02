package com.popIt.pop_it.domain.space.entity;

import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
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
@Table(name = "space")
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String buildingName; // 건물명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RegistrantType registrantType = RegistrantType.OWNER; // 등록자 유형 (기본값: 소유자)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private BuildingType buildingType; // 건물 유형

    @Column(length = 50, nullable = false)
    private String city; // 시/도

    @Column(length = 50, nullable = false)
    private String district; // 시/군/구

    @Column(nullable = false)
    private Double latitude; // 위도

    @Column(nullable = false)
    private Double longitude; // 경도

    @Column(nullable = false)
    private String roadAddress; // 도로명 주소

    @Column(nullable = false)
    private Long deposit; // 보증금

    private Integer pricePerDay; // 일 대여료

    private Integer pricePerWeek; // 주 대여료

    private Integer pricePerMonth; // 월 대여료

    @Column(nullable = false)
    private LocalDate availableStartDate; // 대여 가능 시작일

    @Column(nullable = false)
    private LocalDate availableEndDate; // 대여 가능 종료일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceCategory spaceCategory; // 공간 카테고리

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType spaceType; // 공간 형태

    @Column(nullable = false)
    private Double exclusiveArea; // 전용 면적

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FloorType floorType; // 층 유형

    private Integer floorNumber; // 층수

    @Column(nullable = false)
    private Boolean parkingAvailable; // 주차 가능 여부

    @Column(length = 1000, nullable = false)
    private String description; // 공간 설명

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    private LocalDateTime deletedAt; // 삭제일시

    @Column(nullable = false)
    private Long hostId; // 호스트(등록자) 식별자
}
