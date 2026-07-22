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
}
