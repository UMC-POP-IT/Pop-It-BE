package com.popIt.pop_it.domain.facility.repository;

import com.popIt.pop_it.domain.facility.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findAllByOrderByIdAsc();
}
