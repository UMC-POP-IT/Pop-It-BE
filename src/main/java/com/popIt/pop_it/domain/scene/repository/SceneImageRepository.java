package com.popIt.pop_it.domain.scene.repository;

import com.popIt.pop_it.domain.scene.entity.SceneImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SceneImageRepository extends JpaRepository<SceneImage, Long> {
    // 씬별 이미지 목록 (정렬 순서 유지)
    @Query("""
        select si from SceneImage si
        where si.scene.id = :sceneId
        order by si.sortOrder asc
        """)
    List<SceneImage> findAllBySceneIdOrderBySortOrder(@Param("sceneId") Long sceneId);

    // 기존 사진 뒤에 이어붙일 때 사용할 다음 sortOrder 계산 (사진 없으면 0부터 시작)
    @Query("""
        select coalesce(max(si.sortOrder), -1) + 1 from SceneImage si
        where si.scene.id = :sceneId
        """)
    Integer findNextSortOrder(@Param("sceneId") Long sceneId);
}
