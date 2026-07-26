package com.popIt.pop_it.domain.hotspot.repository;

import com.popIt.pop_it.domain.hotspot.entity.Hotspot;
import org.springframework.data.jpa.repository.Modifying;
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

    // 씬 소프트 삭제 시 그 씬 소속 핫스팟도 같이 정리 (안 지우면 이 씬 소속 LINK 핫스팟이 다른 씬을 계속 참조해
    // 그 다른 씬의 삭제를 영원히 막는 문제가 생김)
    @Modifying
    @Query("delete from Hotspot h where h.scene.id = :sceneId")
    void deleteAllBySceneId(@Param("sceneId") Long sceneId);
}
