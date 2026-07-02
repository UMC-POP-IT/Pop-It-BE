package com.popIt.pop_it.domain.facility.entity;

import com.popIt.pop_it.domain.facility.enums.FacilityCategory;
import com.popIt.pop_it.domain.facility.enums.FacilityName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "facility")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 편의시설 식별자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityCategory category; // 편의시설 분류

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private FacilityName name; // 편의시설명
}
