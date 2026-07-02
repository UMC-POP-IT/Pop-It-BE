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
@Table(name = "space_facility")
public class SpaceFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Space space; // 공간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Facility facility; // 편의시설
}
