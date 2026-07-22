package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpaceFacilityRepository extends JpaRepository<SpaceFacility, Long> {

    // 공간 상세 조회용
    @Query("""
        select sf.facility
        from SpaceFacility sf
        where sf.space.id = :spaceId
        order by sf.facility.id asc
        """)
    List<Facility> findFacilitiesBySpaceId(@Param("spaceId") Long spaceId);
}
