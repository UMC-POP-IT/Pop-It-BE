package com.popIt.pop_it.domain.scene.repository;

import com.popIt.pop_it.domain.scene.entity.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SceneRepository extends JpaRepository<Scene, Long> {

    // 공간별 씬 목록 (soft delete 제외)
    @Query("""
        select s from Scene s
        where s.space.id = :spaceId
        and s.deletedAt is null
        order by s.id asc
        """)
    List<Scene> findAllBySpaceId(@Param("spaceId") Long spaceId);

    // 씬 단건 조회 (soft delete 제외)
    @Query("select s from Scene s where s.id = :sceneId and s.deletedAt is null")
    Optional<Scene> findByIdAndNotDeleted(@Param("sceneId") Long sceneId);

    // 공간의 현재 기본 씬 조회 (기본 씬 재지정 시 기존 것 해제용)
    @Query("""
        select s from Scene s
        where s.space.id = :spaceId
        and s.isDefault = true
        and s.deletedAt is null
        """)
    Optional<Scene> findDefaultBySpaceId(@Param("spaceId") Long spaceId);
}
