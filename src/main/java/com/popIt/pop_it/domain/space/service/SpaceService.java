package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.repository.FacilityRepository;
import com.popIt.pop_it.domain.space.converter.SpaceConverter;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceFacilityRepository;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.HostProfile;
import com.popIt.pop_it.domain.user.repository.HostProfileRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService {
    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final SpaceFacilityRepository spaceFacilityRepository;
    private final FacilityRepository facilityRepository;
    private final HostProfileRepository hostProfileRepository;

    @Transactional
    public SpaceResDTO.CreateResult createSpace(Long userId, SpaceReqDTO.Create request) {

        // 1. 호스트 권한 확인 - 호스트 프로필이 없으면 공간을 등록할 수 없다.
        HostProfile hostProfile = hostProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.HOST_PROFILE_REQUIRED));

        // 2. 계약 가능 기간 검증
        if (request.availableStartDate().isAfter(request.availableEndDate())) {
            throw new ProjectException(SpaceErrorCode.INVALID_AVAILABLE_DATE_RANGE);
        }

        // 3. 공간 본체 저장 (space.host_id = host_profile.id)
        Space space = spaceRepository.save(SpaceConverter.toSpace(request, hostProfile.getId()));

        // 4. 공간 사진 저장 - 요청 배열 순서를 sortOrder로 보존
        List<String> imageUrls = request.imageUrls();
        List<SpaceImage> images = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(SpaceConverter.toSpaceImage(space, imageUrls.get(i), i));
        }

        spaceImageRepository.saveAll(images);

        // 5. 시설 연결 저장 - 요청에 없는 시설 ID가 섞이면 400
        List<Long> facilityIds = request.facilityIds();
        if (facilityIds != null && !facilityIds.isEmpty()) {
            List<Long> distinctIds = facilityIds.stream().distinct().toList();
            List<Facility> facilities = facilityRepository.findAllById(distinctIds);

            if (distinctIds.size() != facilities.size()) {
                throw new ProjectException(SpaceErrorCode.FACILITY_NOT_FOUND);
            }

            List<SpaceFacility> spaceFacilities = facilities.stream()
                    .map(facility -> SpaceConverter.toSpaceFacility(space, facility))
                    .toList();
            spaceFacilityRepository.saveAll(spaceFacilities);
        }

        return SpaceConverter.toCreateResult(space);
    }
}
