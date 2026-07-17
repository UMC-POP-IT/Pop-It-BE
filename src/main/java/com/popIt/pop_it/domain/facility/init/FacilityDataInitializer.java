package com.popIt.pop_it.domain.facility.init;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.enums.FacilityName;
import com.popIt.pop_it.domain.facility.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class FacilityDataInitializer implements ApplicationRunner {

    private final FacilityRepository facilityRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (facilityRepository.count() > 0) {
            return;
        }

        List<Facility> facilities = Arrays.stream(FacilityName.values())
                .map(name -> Facility.builder()
                        .category(name.getCategory())
                        .name(name)
                        .build())
                .toList();

        facilityRepository.saveAll(facilities);
    }
}
