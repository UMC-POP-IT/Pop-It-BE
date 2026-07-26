package com.popIt.pop_it.domain.hotspot.repository;

import com.popIt.pop_it.domain.hotspot.entity.Hotspot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotspotRepository extends JpaRepository<Hotspot, Long> {

    // 씬별 핫스팟 목록 (씬 상세 조회에 포함)
    @Query("select h from Hotspot h where h.scene.id = :sceneId order by h.id asc")
    List<Hotspot> findAllBySceneId(@Param("sceneId") Long sceneId);

    // 다른 씬의 LINK 핫스팟이 이 씬을 참조 중인지 확인 (씬 삭제 시 409 체크용)
    boolean existsByTargetSceneId(Long targetSceneId);
}
