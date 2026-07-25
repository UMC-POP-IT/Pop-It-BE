package com.popIt.pop_it.domain.wishlist.service;

import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.wishlist.converter.WishlistConverter;
import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final SpaceRepository spaceRepository;

    // @Transactional을 두지 않는다: 각 repository 호출을 독립 트랜잭션으로 실행해야
    // saveAndFlush의 유니크 충돌(DataIntegrityViolationException)이 바깥 트랜잭션을
    // rollback-only로 오염시키지 않고 아래 catch로 깔끔하게 전파된다.
    // (toggle은 delete 또는 save 중 하나만 실행하는 단일 쓰기라 하나의 트랜잭션이 필요 없음)
    @Override
    public WishlistResDTO.Toggle toggle(Long userId, Long spaceId) {
        // 존재하지 않거나 삭제된 공간은 찜할 수 없음
        if (spaceRepository.findByIdAndDeletedAtIsNull(spaceId).isEmpty()) {
            throw new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND);
        }

        // 이미 찜한 상태면 해제
        if (wishlistRepository.existsByUserIdAndSpaceId(userId, spaceId)) {
            wishlistRepository.deleteByUserIdAndSpaceId(userId, spaceId);
            return WishlistConverter.toToggle(spaceId, false);
        }

        // 찜하지 않은 상태면 등록
        // saveAndFlush로 INSERT를 즉시 실행 → 유니크 충돌이 커밋까지 지연되지 않고 try 블록 안에서 잡힘
        try {
            wishlistRepository.saveAndFlush(WishlistConverter.toEntity(userId, spaceId));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 선검사를 함께 통과한 경우, (user_id, space_id) 유니크 제약이 최종 방어선 → 이미 등록된 것으로 간주
            return WishlistConverter.toToggle(spaceId, true);
        }
        return WishlistConverter.toToggle(spaceId, true);
    }
}
