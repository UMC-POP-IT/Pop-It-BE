package com.popIt.pop_it.domain.space.entity;

import com.popIt.pop_it.domain.facility.entity.Facility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "space_facility",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_space_facility",
        columnNames = {"space_id", "facility_id"})
)
public class SpaceFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space; // 공간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility; // 편의시설
}
