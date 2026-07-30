package com.popIt.pop_it.domain.wishlist.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.wishlist.converter.WishlistConverter;
import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.domain.user_event.entity.UserEvent;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import com.popIt.pop_it.domain.user_event.repository.UserEventRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.event.UserEngagementEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final UserEventRepository userEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    // toggle은 의도적으로 @Transactional을 두지 않는다: exists/delete/save를 각각 독립된 짧은
    // 트랜잭션으로 즉시 커밋해야 (1) hot key 락 보유시간이 짧아 동시 토글 경합에 강하고,
    // (2) save 유니크 충돌이 바깥 트랜잭션을 rollback-only로 오염시키지 않는다.
    @Override
    public WishlistResDTO.WishlistToggleRes toggle(Long userId, Long spaceId) {
        // 존재하지 않거나 삭제된 공간은 찜할 수 없음
        Space space = spaceRepository.findByIdAndDeletedAtIsNull(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        // 이미 찜한 상태면 해제 (벌크 삭제라 동시 해제에도 멱등)
        if (wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)) {
            wishlistRepository.deleteByUserIdAndSpaceId(userId, spaceId);
            // 삭제 시점에도 여전히 찜 해제 상태일 때만 이벤트를 지운다 (해제/재등록 경합 시 새 이벤트 보존)
            userEventRepository.deleteByUserIdAndSpaceIdAndEventTypeIfNotWishlisted(userId, spaceId, UserEventType.WISHLIST);
            eventPublisher.publishEvent(new UserEngagementEvent(userId));
            return WishlistConverter.toToggle(spaceId, false);
        }

        // 찜하지 않은 상태면 등록. saveAndFlush로 즉시 실행해 유니크 충돌이 이 호출 안에서 잡히도록 한다.
        // 동시 요청이 선검사를 함께 통과한 경우 (user_id, space_id) 유니크 제약이 최종 방어선 → 이미 등록된 것으로 간주.
        try {
            wishlistRepository.saveAndFlush(WishlistConverter.toEntity(userId, spaceId));
        } catch (DataIntegrityViolationException e) {
            return WishlistConverter.toToggle(spaceId, true);
        }
        recordWishlistEvent(userId, spaceId, space);
        eventPublisher.publishEvent(new UserEngagementEvent(userId));
        return WishlistConverter.toToggle(spaceId, true);
    }

    // AI 추천 사유 태그(REGION_PIVOT의 "이 지역 찜했는지" 판단)용 이벤트
    private void recordWishlistEvent(Long userId, Long spaceId, Space space) {
        if (space.getDong() == null) {
            return;
        }
        userEventRepository.save(UserEvent.builder()
                .userId(userId)
                .spaceId(spaceId)
                .eventType(UserEventType.WISHLIST)
                .region(space.getDong())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResDTO.WishlistListRes getMyWishlist(Long userId, int page, int size) {
        // 1. 찜한 공간을 최근 찜한 순으로 페이징 조회 (삭제된 공간 제외)
        Page<Space> spacePage = wishlistRepository.findWishlistedSpaces(userId, PageRequest.of(page, size));

        List<Long> spaceIds = spacePage.getContent().stream()
                .map(Space::getId)
                .toList();

        // 2. 대표 이미지를 한 번의 쿼리로 모아 조회 (N+1 방지)
        Map<Long, String> thumbnailUrlBySpaceId = spaceIds.isEmpty()
                ? Map.of()
                : spaceImageRepository.findThumbnailsBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getSpace().getId(),
                        SpaceImage::getImageUrl,
                        (existing, duplicate) -> existing));

        // 3. 공간별 총 찜 수를 한 번의 쿼리로 집계 (N+1 방지)
        Map<Long, Integer> wishCountBySpaceId = spaceIds.isEmpty()
                ? Map.of()
                : wishlistRepository.countBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        row -> row.getSpaceId(),
                        row -> row.getCount().intValue()));

        return WishlistConverter.toMyWishlistResult(spacePage, thumbnailUrlBySpaceId, wishCountBySpaceId);
    }
}
