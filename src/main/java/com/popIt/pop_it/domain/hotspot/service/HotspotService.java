package com.popIt.pop_it.domain.hotspot.service;

import com.popIt.pop_it.domain.hotspot.converter.HotspotConverter;
import com.popIt.pop_it.domain.hotspot.dto.HotspotReqDTO;
import com.popIt.pop_it.domain.hotspot.dto.HotspotResDTO;
import com.popIt.pop_it.domain.hotspot.entity.Hotspot;
import com.popIt.pop_it.domain.hotspot.enums.HotspotType;
import com.popIt.pop_it.domain.hotspot.exception.HotspotException;
import com.popIt.pop_it.domain.hotspot.exception.code.HotspotErrorCode;
import com.popIt.pop_it.domain.hotspot.repository.HotspotRepository;
import com.popIt.pop_it.domain.scene.entity.Scene;
import com.popIt.pop_it.domain.scene.exception.code.SceneErrorCode;
import com.popIt.pop_it.domain.scene.repository.SceneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HotspotService {

    private final HotspotRepository hotspotRepository;
    private final SceneRepository sceneRepository;

    //핫스팟 생성
    //원본 씬(sceneId)에 락을 걸어서 SceneCommandService.deleteScene과 같은 row를 두고 직렬화
    //(락 없이 조회만 하면, 삭제 트랜잭션이 참조 체크를 통과한 직후 이 트랜잭션이 커밋되며
    //  삭제된 씬 위에 핫스팟이 저장되는 경합이 생길 수 있었음)
    public HotspotResDTO.HotspotIdRes createHotspot(Long sceneId, Long hostId, HotspotReqDTO.HotspotCreateReq request) {
        Scene scene = sceneRepository.findByIdForUpdate(sceneId)
                .orElseThrow(() -> new HotspotException(SceneErrorCode.SCENE_NOT_FOUND));

        validateHost(scene, hostId);
        validateTypeFields(request.type(), request.description(), request.targetSceneId());
        if (request.type() == HotspotType.LINK) {
            validateTargetScene(scene, request.targetSceneId());
        }

        Hotspot hotspot = Hotspot.builder()
                .scene(scene)
                .positionX(request.positionX())
                .positionY(request.positionY())
                .positionZ(request.positionZ())
                .label(request.label())
                .type(request.type())
                .description(request.type() == HotspotType.INFO ? request.description() : null)
                .targetSceneId(request.type() == HotspotType.LINK ? request.targetSceneId() : null)
                .build();
        hotspotRepository.save(hotspot);

        return HotspotConverter.toHotspotId(hotspot);
    }

    //핫스팟 부분 수정 (type 자체는 변경 불가)
    public HotspotResDTO.HotspotIdRes updateHotspot(Long hotspotId, Long hostId, HotspotReqDTO.HotspotUpdateReq request) {
        Hotspot hotspot = hotspotRepository.findById(hotspotId)
                .orElseThrow(() -> new HotspotException(HotspotErrorCode.HOTSPOT_NOT_FOUND));

        validateHost(hotspot.getScene(), hostId);

        if (hotspot.getType() == HotspotType.INFO && request.targetSceneId() != null) {
            throw new HotspotException(HotspotErrorCode.HOTSPOT_TYPE_FIELD_MISMATCH);
        }
        if (hotspot.getType() == HotspotType.LINK && request.description() != null) {
            throw new HotspotException(HotspotErrorCode.HOTSPOT_TYPE_FIELD_MISMATCH);
        }
        if (hotspot.getType() == HotspotType.LINK && request.targetSceneId() != null) {
            validateTargetScene(hotspot.getScene(), request.targetSceneId());
        }

        hotspot.update(request.positionX(), request.positionY(), request.positionZ(),
                request.label(), request.description(), request.targetSceneId());

        return HotspotConverter.toHotspotId(hotspot);
    }

    //핫스팟 삭제 (hard delete)
    public void deleteHotspot(Long hotspotId, Long hostId) {
        Hotspot hotspot = hotspotRepository.findById(hotspotId)
                .orElseThrow(() -> new HotspotException(HotspotErrorCode.HOTSPOT_NOT_FOUND));

        validateHost(hotspot.getScene(), hostId);

        hotspotRepository.delete(hotspot);
    }

    //본인 소유 공간인지 확인
    private void validateHost(Scene scene, Long hostId) {
        if (!scene.getSpace().getHostId().equals(hostId)) {
            throw new HotspotException(HotspotErrorCode.HOTSPOT_ACCESS_DENIED);
        }
    }

    //type별 필수 필드 확인 (INFO -> description, LINK -> targetSceneId)
    private void validateTypeFields(HotspotType type, String description, Long targetSceneId) {
        if (type == HotspotType.INFO && (description == null || description.isBlank())) {
            throw new HotspotException(HotspotErrorCode.HOTSPOT_TYPE_FIELD_MISMATCH);
        }
        if (type == HotspotType.LINK && targetSceneId == null) {
            throw new HotspotException(HotspotErrorCode.HOTSPOT_TYPE_FIELD_MISMATCH);
        }
    }

    //LINK 타입의 targetSceneId 검증 - 자기 자신을 가리키지 않는지 + 실제 존재하는(삭제 안 된) 씬인지
    //+ 같은 공간(Space) 소속인지 (다른 공간의 씬으로는 링크 불가 - 존재 자체를 숨기기 위해 404로 통일)
    //비관적 락으로 잡아서, 이 트랜잭션이 끝날 때까지 해당 씬이 삭제되지 못하게 함
    //(SceneCommandService.deleteScene도 동일하게 findByIdForUpdate를 써서 같은 씬 row를 두고 직렬화됨)
    private void validateTargetScene(Scene sourceScene, Long targetSceneId) {
        if (targetSceneId.equals(sourceScene.getId())) {
            throw new HotspotException(HotspotErrorCode.HOTSPOT_SELF_REFERENCE);
        }

        Scene targetScene = sceneRepository.findByIdForUpdate(targetSceneId)
                .orElseThrow(() -> new HotspotException(HotspotErrorCode.HOTSPOT_TARGET_SCENE_NOT_FOUND));

        if (!targetScene.getSpace().getId().equals(sourceScene.getSpace().getId())) {
            throw new HotspotException(HotspotErrorCode.HOTSPOT_TARGET_SCENE_NOT_FOUND);
        }
    }
}
