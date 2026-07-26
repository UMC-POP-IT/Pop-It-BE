package com.popIt.pop_it.domain.scene.service;

import com.popIt.pop_it.domain.hotspot.entity.Hotspot;
import com.popIt.pop_it.domain.hotspot.repository.HotspotRepository;
import com.popIt.pop_it.domain.scene.converter.SceneConverter;
import com.popIt.pop_it.domain.scene.dto.SceneResDTO;
import com.popIt.pop_it.domain.scene.entity.Scene;
import com.popIt.pop_it.domain.scene.exception.SceneException;
import com.popIt.pop_it.domain.scene.exception.code.SceneErrorCode;
import com.popIt.pop_it.domain.scene.repository.SceneRepository;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneQueryService {
    private final SceneRepository sceneRepository;
    private final SpaceRepository spaceRepository;
    private final HotspotRepository hotspotRepository;

    //공간의 방(씬) 목록 조회
    public SceneResDTO.SceneListRes getScenes(Long spaceId) {
        if (!spaceRepository.existsByIdAndDeletedAtIsNull(spaceId)) {
            throw new SceneException(SpaceErrorCode.SPACE_NOT_FOUND);
        }

        return SceneConverter.toSceneList(sceneRepository.findAllBySpaceId(spaceId));
    }

    //방(씬) 상세 조회 (모델·카메라 설정·핫스팟 목록 포함)
    public SceneResDTO.SceneDetailRes getSceneDetail(Long sceneId) {
        Scene scene = sceneRepository.findByIdAndNotDeleted(sceneId)
                .orElseThrow(() -> new SceneException(SceneErrorCode.SCENE_NOT_FOUND));

        List<Hotspot> hotspots = hotspotRepository.findAllBySceneId(sceneId);

        return SceneConverter.toDetail(scene, hotspots);
    }
}
