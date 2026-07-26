package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.repository.FacilityRepository;
import com.popIt.pop_it.domain.space.converter.SpaceConverter;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceFacilityRepository;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.repository.HostProfileRepository;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService {
    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final SpaceFacilityRepository spaceFacilityRepository;
    private final FacilityRepository facilityRepository;
    private final HostProfileRepository hostProfileRepository;
    private final WishlistRepository wishlistRepository;
    private final KakaoLocalService kakaoLocalService;

    @Transactional
    public SpaceResDTO.SpaceCreateRes createSpace(Long userId, SpaceReqDTO.SpaceCreateReq request) {

        // 1. 호스트 권한 확인 - 호스트 프로필이 없으면 공간을 등록할 수 없다.
        if (!hostProfileRepository.existsByUserId(userId)) {
            throw new ProjectException(SpaceErrorCode.HOST_PROFILE_REQUIRED);
        }

        // 2. 계약 가능 기간 검증
        if (request.availableStartDate().isAfter(request.availableEndDate())) {
            throw new ProjectException(SpaceErrorCode.INVALID_AVAILABLE_DATE_RANGE);
        }

        // 3. 좌표 -> 동 변환 후 공간 본체 저장 (space.host_id = user.id)
        // 카카오 api 실패 시 dong = null로 저장하고 등록은 정상 진행
        String dong = kakaoLocalService.resolveDong(request.latitude(), request.longitude()).orElse(null);
        Space space = spaceRepository.save(SpaceConverter.toSpace(request, userId, dong));

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

    // 공간 상세 조회
    public SpaceResDTO.SpaceDetailRes getSpaceDetail(Long userId, Long spaceId) {
        // 1. 공간 조회
        Space space = spaceRepository.findByIdAndDeletedAtIsNull(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        // 2. 사진, 시설 조회
        List<String> imageUrls = spaceImageRepository.findImageUrlsBySpaceId(spaceId);
        List<Facility> facilities = spaceFacilityRepository.findFacilitiesBySpaceId(spaceId);

        // 3. 총 찜 수는 로그인 여부와 상관 없이 항상 나타남
        int wishCount = (int) wishlistRepository.countBySpaceId(spaceId);

        // 4. isMine, isWishlisted 는 로그인한 경우에만 계산 (비로그인이면 둘 다 false)
        boolean isMine = false;
        boolean isWishlist = false;

        if (userId != null) {
            isMine = space.getHostId().equals(userId);
            isWishlist = wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId);
        }

        return SpaceConverter.toDetail(space, imageUrls, facilities, isMine, isWishlist, wishCount);
    }

    // 내 공간 목록 조회 (호스트)
    public SpaceResDTO.MySpaceListRes getMySpaces(Long userId, int page, int size) {
        // 1. 호스트 권한 확인 - 호스트 프로필이 없으면 내 공간 자체가 존재 X
        if (!hostProfileRepository.existsByUserId(userId)) {
            throw new ProjectException(SpaceErrorCode.HOST_PROFILE_REQUIRED);
        }

        // 2. 내 공간 페이징 조회
        Page<Space> spacePage = spaceRepository.findAllByHostIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        // 3. 대표 이미지를 한 번의 쿼리로 모아서 조회
        List<Long> spaceIds = spacePage.getContent().stream()
                .map(space -> space.getId())
                .toList();

        Map<Long, String> thumbnailUrlBySpaceId = spaceIds.isEmpty()
                ? Map.of()
                : spaceImageRepository.findThumbnailsBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getSpace().getId(),
                        image -> image.getImageUrl(),
                        (existing, duplicate) -> existing));

        return SpaceConverter.toMyListResult(spacePage, thumbnailUrlBySpaceId);
    }

    public SpaceResDTO.SpaceSearchListRes searchSpaces(Long userId, SpaceReqDTO.SpaceSearchReq request) {

        // 1. 빈 문자열은 필터 미적용으로 취급
        String keyword = blankToNull(request.keyword());
        String district = blankToNull(request.district());

        // 2. 검색어가 공간 정보의 한글 이름과 겹치면 그 enum으로도 검색되게 함
        SpaceCategory keywordCategory = matchCategory(keyword);
        SpaceType keywordType = matchType(keyword);

        // 3. 조건에 맞는 공간 페이징 조회
        Page<Space> spacePage = spaceRepository.search(
                keyword,
                district,
                request.spaceCategory(),
                keywordCategory != null,
                keywordCategory,
                keywordType != null,
                keywordType,
                PageRequest.of(request.page(), request.size())
        );

        List<Long> spaceIds = spacePage.getContent().stream()
                .map(Space::getId)
                .toList();

        if (spaceIds.isEmpty()) {
            return SpaceConverter.toSearchResult(spacePage, Map.of(), Map.of(), Set.of());
        }

        // 4. 대표 이미지 조회
        Map<Long, String> thumbnailUrlBySpaceId = spaceImageRepository.findThumbnailsBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getSpace().getId(),
                        image -> image.getImageUrl(),
                        (existing, duplicate) -> existing));

        // 5. 찜 수 조회
        Map<Long, Integer> wishCountBySpaceId = wishlistRepository.countBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        WishlistRepository.WishCountView::getSpaceId,
                        view -> view.getWishCount().intValue()));

        // 6. 내 찜 여부 조회
        Set<Long> wishlistedSpaceIds = (userId == null)
                ? Set.of()
                : Set.copyOf(wishlistRepository.findWishlistedSpaceIds(userId, spaceIds));

        return SpaceConverter.toSearchResult(spacePage, thumbnailUrlBySpaceId, wishCountBySpaceId, wishlistedSpaceIds);
    }

    // 검색어와 공간 용도(카테고리)의 한글 이름 대조 예) "팝업" -> POPUP_STORE
    private static SpaceCategory matchCategory(String keyword) {
        String normalized = normalizeForMatch(keyword);
        if (normalized == null) {
            return null;
        }

        return Arrays.stream(SpaceCategory.values())
                .filter(category -> normalizeForMatch(category.getDescription()).contains(normalized))
                .findFirst()
                .orElse(null);
    }

    // 검색어와 공간 구조 유형의 한글 이름 대조 예) "오픈형" -> OPEN_HALL
    private static SpaceType matchType(String keyword) {
        String normalized = normalizeForMatch(keyword);
        if (normalized == null) {
            return null;
        }

        return Arrays.stream(SpaceType.values())
                .filter(spaceType -> normalizeForMatch(spaceType.getDescription()).contains(normalized))
                .findFirst()
                .orElse(null);
    }

    // 공백과 대소문자 차이를 무시하고 비교하기 위한 정규화 ("오픈형 홀" 과 "오픈형홀" 을 같게 취급)
    private static String normalizeForMatch(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.replaceAll("\\s+", "").toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
