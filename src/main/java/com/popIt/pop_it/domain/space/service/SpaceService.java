package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.facility.entity.Facility;
import com.popIt.pop_it.domain.facility.repository.FacilityRepository;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.converter.SpaceConverter;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceFacility;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.enums.RealtimeRecommendType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.event.SpaceViewedEvent;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceFacilityRepository;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.HostProfileRepository;
import com.popIt.pop_it.domain.wishlist.repository.WishCountBySpace;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.event.SpaceCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final ApplicationEventPublisher eventPublisher;
    private final ReservationRepository reservationRepository;
    private final SpaceUtilizationCalculator spaceUtilizationCalculator;

    // 공간 삭제를 막아야하는 예약 상태
    private static final List<ReservationStatus> BLOCKING_RESERVATION_STATUSES = List.of(
            ReservationStatus.PENDING_APPROVAL,
            ReservationStatus.APPROVED,
            ReservationStatus.CONTRACT_COMPLETED,
            ReservationStatus.PAYMENT_COMPLETED,
            ReservationStatus.IN_USE,
            ReservationStatus.USAGE_COMPLETED
    );

    // 실시간 추천 캐러셀에 한 번에 내려줄 최대 공간 수
    private static final int REALTIME_RECOMMENDED_POOL_SIZE = 20;
    // 캐러셀 앞단에 추천 유형 공간을 배치할 슬롯 수 (Slot 1~3)
    private static final int FRONT_SLOT_SIZE = 3;

    @Transactional
    public SpaceResDTO.SpaceCreateRes createSpace(Long userId, SpaceReqDTO.SpaceCreateReq request) {

        // 1. 호스트 권한 확인 - 호스트 프로필이 없으면 공간을 등록할 수 없다.
        if (!hostProfileRepository.existsByUserId(userId)) {
            throw new ProjectException(SpaceErrorCode.HOST_PROFILE_REQUIRED);
        }

        // 2. 좌표 -> 동 변환 후 공간 본체 저장 (space.host_id = user.id)
        // 카카오 api 실패 시 dong = null로 저장하고 등록은 정상 진행
        String dong = kakaoLocalService.resolveDong(request.latitude(), request.longitude()).orElse(null);
        Space space = spaceRepository.save(SpaceConverter.toSpace(request, userId, dong));

        // 3. 공간 사진 저장 - 요청 배열 순서를 sortOrder로 보존
        List<String> imageUrls = request.imageUrls();
        List<SpaceImage> images = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(SpaceConverter.toSpaceImage(space, imageUrls.get(i), i));
        }

        spaceImageRepository.saveAll(images);

        // 4. 시설 연결 저장 - 요청에 없는 시설 ID가 섞이면 400
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

        // 5. AI 추천용 임베딩 생성 - 커밋 후(AFTER_COMMIT)에 처리해야 함.
        eventPublisher.publishEvent(new SpaceCreatedEvent(space.getId()));

        return SpaceConverter.toCreateResult(space);
    }

    // 공간 상세 조회
    public SpaceResDTO.SpaceDetailRes getSpaceDetail(Long userId, Long spaceId, UserMode viewerMode) {
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

            eventPublisher.publishEvent(new SpaceViewedEvent(spaceId, userId, viewerMode));
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
                        WishCountBySpace::getSpaceId,
                        view -> view.getCount().intValue()));

        // 6. 내 찜 여부 조회
        Set<Long> wishlistedSpaceIds = (userId == null)
                ? Set.of()
                : Set.copyOf(wishlistRepository.findWishlistedSpaceIds(userId, spaceIds));

        return SpaceConverter.toSearchResult(spacePage, thumbnailUrlBySpaceId, wishCountBySpaceId, wishlistedSpaceIds);
    }

    // 공간 수정 (전달된 필드만 반영, 리스트는 전체 교체)
    @Transactional
    public SpaceResDTO.SpaceUpdateRes updateSpace(Long userId, Long spaceId, SpaceReqDTO.SpaceUpdateReq request) {
        // 1. 공간 조회, 소유권 확인
        Space space = spaceRepository.findByIdAndDeletedAtIsNull(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        if (!space.getHostId().equals(userId)) {
            throw new ProjectException(SpaceErrorCode.NOT_SPACE_OWNER);
        }

        // 2. 좌표는 위도, 경도를 한 세트로만 수정 가능
        boolean hasLatitude = request.latitude() != null;
        boolean hasLongitude = request.longitude() != null;

        if (hasLatitude != hasLongitude) {
            throw new ProjectException(SpaceErrorCode.INVALID_COORDINATE_PAIR);
        }

        // 3. 계약 가능 기간은 요청값 + 기존값을 합친 최종 상태로 검증
        LocalDate startDate = (request.availableStartDate() != null)
                ? request.availableStartDate()
                : space.getAvailableStartDate();
        LocalDate endDate = (request.availableEndDate() != null)
                ? request.availableEndDate()
                : space.getAvailableEndDate();

        if (startDate.isAfter(endDate)) {
            throw new ProjectException(SpaceErrorCode.INVALID_AVAILABLE_DATE_RANGE);
        }

        // 4. 교체할 시설을 먼저 검증 (지우고 나서 실패하는 상황을 만들지 않도록 삭제보다 앞에 둠)
        List<Facility> facilities = null;
        if (request.facilityIds() != null) {
            List<Long> distinctIds = request.facilityIds().stream().distinct().toList();
            facilities = distinctIds.isEmpty() ? List.of() : facilityRepository.findAllById(distinctIds);

            if (distinctIds.size() != facilities.size()) {
                throw new ProjectException(SpaceErrorCode.FACILITY_NOT_FOUND);
            }
        }

        // 5. 좌표가 바뀌면 동도 다시 계산
        String dong = hasLatitude
                ? kakaoLocalService.resolveDong(request.latitude(), request.longitude()).orElse(null)
                : null;

        // 6. 공간 본체 수정
        space.update(
                request.buildingName(),
                request.registrantType(),
                request.buildingType(),
                request.city(),
                request.district(),
                request.roadAddress(),
                request.addressDetail(),
                request.deposit(),
                request.pricePerDay(),
                request.availableStartDate(),
                request.availableEndDate(),
                request.spaceCategory(),
                request.spaceType(),
                request.exclusiveArea(),
                request.parkingAvailable(),
                request.description()
        );
        space.updateLocation(request.latitude(), request.longitude(), dong);
        space.updateFloorInfo(request.floorType(), request.floorNumber());

        // 7. 시설 전체 교체 (요청에 facilityIds가 있는 경우에만)
        if (facilities != null) {
            spaceFacilityRepository.deleteAllBySpaceId(spaceId);

            if (!facilities.isEmpty()) {
                spaceFacilityRepository.saveAll(facilities.stream()
                        .map(facility -> SpaceConverter.toSpaceFacility(space, facility))
                        .toList());
            }
        }

        // 8. 사진 전체 교체 (요청 배열 순서를 sortOrder로 다시 부여)
        if (request.imageUrls() != null) {
            spaceImageRepository.deleteAllBySpaceId(spaceId);

            List<String> imageUrls = request.imageUrls();
            List<SpaceImage> images = new ArrayList<>();

            for (int i = 0; i < imageUrls.size(); i++) {
                images.add(SpaceConverter.toSpaceImage(space, imageUrls.get(i), i));
            }

            spaceImageRepository.saveAll(images);
        }

        return SpaceConverter.toUpdateResult(space);
    }

    // 공간 삭제 (소프트 삭제)
    @Transactional
    public SpaceResDTO.SpaceDeleteRes deleteSpace(Long userId, Long spaceId) {

        // 1. 공간 행을 비관적 쓰기 락으로 조회한다.
        //    -> 예약 생성(ReservationCommandService)도 같은 findByIdForUpdate로 이 행을 잡기 때문에
        //    -> '예약 생성 중'과 '삭제'가 같은 공간에서 동시에 진행되지 못하고 직렬화된다.
        Space space;
        try {
            space = spaceRepository.findByIdForUpdate(spaceId)
                    .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            // 다른 트랜젝샨(ex. 예약 생성)이 이 공간을 선점 중 -> 지금은 삭제 불가능
            throw new ProjectException(SpaceErrorCode.SPACE_HAS_ACTIVE_RESERVATION);
        }

        // 2. findByIdForUpdate는 deleteAt을 거르지 않으므로 직접 확인
        if (space.getDeletedAt() != null) {
            throw new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND);
        }

        // 3. 소유권 확인
        if (!space.getHostId().equals(userId)) {
            throw new ProjectException(SpaceErrorCode.NOT_SPACE_OWNER);
        }

        // 4. 종료되지 않은 예약인 경우 삭제 불가능
        if (reservationRepository.existsBySpaceIdAndStatusIn(spaceId, BLOCKING_RESERVATION_STATUSES)) {
            throw new ProjectException(SpaceErrorCode.SPACE_HAS_ACTIVE_RESERVATION);
        }

        // 3. 행을 지우지 않고 deleteAt만 기록 (soft delete)
        space.softDelete();

        return SpaceConverter.toDeleteResult(space);
    }

    // 실시간 추천 공간 목록 조회
    public SpaceResDTO.SpaceRealtimeRecommendedListRes getRealtimeRecommendedSpaces() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 추천 풀 조회
        List<Space> candidates = spaceRepository.findAllActiveForRecommendation();

        if (candidates.isEmpty()) {
            return SpaceConverter.toRealtimeRecommendedList(List.of(), Map.of(), Map.of());
        }

        // 2. 가동률 하위 공간 판정
        SpaceUtilizationCalculator.UtilizationResult utilization =
                spaceUtilizationCalculator.calculate(candidates, now);

        // 3. 추천 유형 분류
        Map<Long, RealtimeRecommendType> typeBySpaceId = candidates.stream()
                .collect(Collectors.toMap(
                        Space::getId,
                        space -> RealtimeRecommendType.classify(
                                space.getCreatedAt(),
                                now,
                                utilization.isLowUtilization(space.getId()))));

        // 4. 슬롯 배치 후 캐러셀에 내려줄 개수만큼 자르기
        List<Space> orderedSpaces = placeIntoSlots(candidates, typeBySpaceId, utilization).stream()
                .limit(REALTIME_RECOMMENDED_POOL_SIZE)
                .toList();

        // 5. 최종 선정된 공간의 대표 이미지 조회
        List<Long> spaceIds = orderedSpaces.stream()
                .map(Space::getId)
                .toList();

        Map<Long, String> thumbnailUrlBySpaceId = spaceImageRepository.findThumbnailsBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getSpace().getId(),
                        image -> image.getImageUrl(),
                        (existing, duplicate) -> existing));

        return SpaceConverter.toRealtimeRecommendedList(orderedSpaces, typeBySpaceId, thumbnailUrlBySpaceId);
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

    // 슬롯 배치
    private static List<Space> placeIntoSlots(
            List<Space> spaces,
            Map<Long, RealtimeRecommendType> typeBySpaceId,
            SpaceUtilizationCalculator.UtilizationResult utilization
    ) {
        // 앞단 (Slot 1~3) 후보
        List<Space> frontCandidates = new ArrayList<>();
        frontCandidates.addAll(filterByType(spaces, typeBySpaceId, RealtimeRecommendType.LOW_UTILIZATION));
        frontCandidates.addAll(filterByType(spaces, typeBySpaceId, RealtimeRecommendType.NEW));

        List<Space> ordered = new ArrayList<>(frontCandidates.stream()
                .limit(FRONT_SLOT_SIZE)
                .toList());

        Set<Long> placeIds = ordered.stream()
                .map(Space::getId)
                .collect(Collectors.toSet());

        // 남는 칸: 기본 공간 먼저
        ordered.addAll(spaces.stream()
                .filter(space -> !placeIds.contains(space.getId()))
                .filter(space -> !isFrontSlotType(typeBySpaceId, space))
                .sorted(Comparator.comparingLong(
                        (Space space) -> utilization.viewCountOf(space.getId())).reversed())
                .toList());

        // 기본 공간이 부족하면 앞단에 못 들어간 추천 공간으로 마저 채움
        ordered.addAll(spaces.stream()
                .filter(space -> !placeIds.contains(space.getId()))
                .filter(space -> isFrontSlotType(typeBySpaceId, space))
                .toList());

        return ordered;
    }

    private static List<Space> filterByType(
            List<Space> spaces,
            Map<Long, RealtimeRecommendType> typeBySpaceId,
            RealtimeRecommendType type
    ) {
        return spaces.stream()
                .filter(space -> typeBySpaceId.get(space.getId()) == type)
                .toList();
    }

    private static boolean isFrontSlotType(
            Map<Long, RealtimeRecommendType> typeBySpaceId,
            Space space
    ) {
        RealtimeRecommendType type = typeBySpaceId.get(space.getId());
        return type != null && type.isFrontSlotType();
    }
}
