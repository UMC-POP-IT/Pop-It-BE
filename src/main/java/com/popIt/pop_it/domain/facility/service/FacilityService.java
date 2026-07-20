package com.popIt.pop_it.domain.facility.service;

import com.popIt.pop_it.domain.facility.converter.FacilityConverter;
import com.popIt.pop_it.domain.facility.dto.FacilityResDTO;
import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityResDTO.ListResult getFacilities() {
        List<Facility> facilities = facilityRepository.findAllByOrderByIdAsc();
        return FacilityConverter.toListResult(facilities);
    }
}