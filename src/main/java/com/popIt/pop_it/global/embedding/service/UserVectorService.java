package com.popIt.pop_it.global.embedding.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user_activity.entity.UserActivity;
import com.popIt.pop_it.domain.user_activity.repository.UserActivityRepository;
import com.popIt.pop_it.domain.wishlist.entity.Wishlist;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.embedding.redis.UserVectorRedisStore;
import com.popIt.pop_it.global.util.EngagementType;
import com.popIt.pop_it.global.util.TimeDecayCalculator;
import com.popIt.pop_it.global.util.VectorMath;
import com.popIt.pop_it.global.util.VectorMath.WeightedVector;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저의 전체 찜/조회 이력을 시간 감쇠 가중평균해 취향 벡터를 계산하고 Redis에 저장한다.
 * 기간으로 이력을 자르지 않고 TimeDecayCalculator의 지수 감쇠로 오래된 이력의 영향력만 줄인다.
 * 찜/조회 이력이 전혀 없는 유저(신규 유저)는 취향 벡터를 만들지 않는다.
 * Gemini API는 재호출하지 않고, 이미 저장된 공간 임베딩만 재사용한다.
 *
 * UserVectorRecomputeListener의 AFTER_COMMIT 콜백에서 호출되므로 REQUIRES_NEW가 필요하다:
 * 그 시점엔 원래 트랜잭션이 커밋된 뒤지만 Spring 트랜잭션 동기화 상태는 아직 안 지워져 있어서,
 * 기본 전파(REQUIRED)를 쓰면 새 트랜잭션 대신 그 죽은 동기화 컨텍스트에 참여하려다 실패할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class UserVectorService {

    private final WishlistRepository wishlistRepository;
    private final UserActivityRepository userActivityRepository;
    private final SpaceRepository spaceRepository;
    private final UserVectorRedisStore userVectorRedisStore;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void recomputeUserVector(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        List<Wishlist> wishlists = wishlistRepository.findByUserId(userId);
        List<UserActivity> activities = userActivityRepository.findByUserId(userId);

        if (wishlists.isEmpty() && activities.isEmpty()) {
            userVectorRedisStore.delete(userId); // 이력이 아예 없어졌으면 예전에 캐시된 벡터도 정리
            return;
        }

        Set<Long> spaceIds = new HashSet<>();
        wishlists.forEach(w -> spaceIds.add(w.getSpaceId()));
        activities.forEach(a -> spaceIds.add(a.getSpaceId()));

        Map<Long, Space> spaceById = spaceRepository.findAllById(spaceIds).stream()
                .collect(Collectors.toMap(Space::getId, Function.identity()));

        List<WeightedVector> weightedVectors = new ArrayList<>();
        for (Wishlist wishlist : wishlists) {
            addWeightedVector(weightedVectors, spaceById.get(wishlist.getSpaceId()),
                    wishlist.getCreatedAt(), now, EngagementType.WISHLIST);
        }
        for (UserActivity activity : activities) {
            addWeightedVector(weightedVectors, spaceById.get(activity.getSpaceId()),
                    activity.getLastViewedAt(), now, EngagementType.VIEW);
        }

        if (weightedVectors.isEmpty()) {
            userVectorRedisStore.delete(userId); // 임베딩 없는 공간만 남았다면 이전 벡터도 더는 유효하지 않음
            return;
        }

        float[] userVector = VectorMath.weightedAverage(weightedVectors);
        userVectorRedisStore.save(userId, userVector);
    }

    public Optional<float[]> getUserVector(Long userId) {
        return userVectorRedisStore.find(userId);
    }

    private void addWeightedVector(List<WeightedVector> target, Space space,
                                    LocalDateTime eventTime, LocalDateTime now, EngagementType type) {
        if (space == null || space.getEmbedding() == null) {
            return;
        }
        double weight = TimeDecayCalculator.decay(eventTime, now) * TimeDecayCalculator.actionWeight(type);
        target.add(new WeightedVector(space.getEmbedding(), weight));
    }
}
