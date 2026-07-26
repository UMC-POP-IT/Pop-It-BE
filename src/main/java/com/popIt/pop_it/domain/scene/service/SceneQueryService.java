package com.popIt.pop_it.domain.scene.service;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneQueryService {
    private final SceneRepository sceneRepository;
    private final SpaceRepository spaceRepository;

    //공간의 방(씬) 목록 조회
    public SceneResDTO.SceneList getScenes(Long spaceId) {
        if (!spaceRepository.existsByIdAndDeletedAtIsNull(spaceId)) {
            throw new SceneException(SpaceErrorCode.SPACE_NOT_FOUND);
        }

        return SceneConverter.toSceneList(sceneRepository.findAllBySpaceId(spaceId));
    }

    //방(씬) 상세 조회 (모델·카메라 설정 포함, 핫스팟은 후속 이슈)
    public SceneResDTO.Detail getSceneDetail(Long sceneId) {
        Scene scene = sceneRepository.findByIdAndNotDeleted(sceneId)
                .orElseThrow(() -> new SceneException(SceneErrorCode.SCENE_NOT_FOUND));

        return SceneConverter.toDetail(scene);
    }
}
