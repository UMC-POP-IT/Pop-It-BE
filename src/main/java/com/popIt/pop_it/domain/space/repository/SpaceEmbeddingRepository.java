package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.Space;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceEmbeddingRepository extends JpaRepository<Space, Long> {

    // AI 추천용(유사도 랭킹) - 80개 규모라 인덱스 없이 전량 조회 후 애플리케이션에서 순차 스캔
    List<Space> findAllByEmbeddingIsNotNullAndDeletedAtIsNull();
}
