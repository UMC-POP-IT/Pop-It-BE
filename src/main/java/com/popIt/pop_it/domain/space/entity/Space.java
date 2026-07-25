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

    @Column(length = 50)
    private String dong;

    @Column(nullable = false)
    private Double latitude; // 위도

    @Column(nullable = false)
    private Double longitude; // 경도

    @Column(nullable = false)
    private String roadAddress; // 도로명 주소

    @Column(length = 30, nullable = false)
    private String addressDetail; // 상세 주소

    @Column(nullable = false)
    private Long deposit; // 보증금

    @Column(nullable = false)
    private Integer pricePerDay; // 일 대여료

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
    private Long hostId; // 공간을 등록한 호스트의 user.id (host_profile.id 아님)

    // ==== 도메인 메서드 ====

    // 공간 수정 (값이 들어온 필드만 반영 -> null: 기존 값 유지)
    // space 테이블은 floorNumber를 제외한 스칼라 컬럼이 전부 NOT NULL이라 "null = 제거"가 성립하지 않음 -> 따로 처리
    public void update(
            String buildingName,
            RegistrantType registrantType,
            BuildingType buildingType,
            String city,
            String district,
            String roadAddress,
            String addressDetail,
            Long deposit,
            Integer pricePerDay,
            LocalDate availableStartDate,
            LocalDate availableEndDate,
            SpaceCategory spaceCategory,
            SpaceType spaceType,
            Double exclusiveArea,
            Boolean parkingAvailable,
            String description
    ) {
        this.buildingName = orKeep(buildingName, this.buildingName);
        this.registrantType = orKeep(registrantType, this.registrantType);
        this.buildingType = orKeep(buildingType, this.buildingType);
        this.city = orKeep(city, this.city);
        this.district = orKeep(district, this.district);
        this.roadAddress = orKeep(roadAddress, this.roadAddress);
        this.addressDetail = orKeep(addressDetail, this.addressDetail);
        this.deposit = orKeep(deposit, this.deposit);
        this.pricePerDay = orKeep(pricePerDay, this.pricePerDay);
        this.availableStartDate = orKeep(availableStartDate, this.availableStartDate);
        this.availableEndDate = orKeep(availableEndDate, this.availableEndDate);
        this.spaceCategory = orKeep(spaceCategory, this.spaceCategory);
        this.spaceType = orKeep(spaceType, this.spaceType);
        this.exclusiveArea = orKeep(exclusiveArea, this.exclusiveArea);
        this.parkingAvailable = orKeep(parkingAvailable, this.parkingAvailable);
        this.description = orKeep(description, this.description);
    }

    // 층 정보 수정 (floorType과 floorNumber는 한 세트로 처리)
    public void updateFloorInfo(FloorType floorType, Integer floorNumber) {
        if (floorType == null) {
            return;
        }

        this.floorType = floorType;
        this.floorNumber = floorNumber;
    }

    // 좌표 수정 (위도, 경도는 한 세트로 처리)
    // dong은 좌표에서 파생되는 값 -> 좌표가 바뀌면 다시 계산된 값으로 덮어씀
    public void updateLocation(Double latitude, Double longitude, String dong) {
        if (latitude == null || longitude == null) {
            return;
        }

        this.latitude = latitude;
        this.longitude = longitude;
        this.dong = dong;
    }

    // 요청값이 있으면 그 값으로, 없으면 기존 값 그대로 유지
    private static <T> T orKeep(T requested, T current) {
        return (requested != null) ? requested : current;
    }

    // 소프트 삭제 (공간 삭제 시 행을 지우지 않고 deletedAt에만 기록)
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
