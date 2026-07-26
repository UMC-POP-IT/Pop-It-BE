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
    public HotspotResDTO.HotspotIdRes createHotspot(Long sceneId, Long hostId, HotspotReqDTO.HotspotCreateReq request) {
        Scene scene = sceneRepository.findByIdAndNotDeleted(sceneId)
                .orElseThrow(() -> new HotspotException(SceneErrorCode.SCENE_NOT_FOUND));

        validateHost(scene, hostId);
        validateTypeFields(request.type(), request.description(), request.targetSceneId());
        if (request.type() == HotspotType.LINK) {
            validateTargetSceneExists(request.targetSceneId());
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
            validateTargetSceneExists(request.targetSceneId());
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

    //LINK 타입의 targetSceneId가 실제 존재하는(삭제 안 된) 씬인지 확인
    private void validateTargetSceneExists(Long targetSceneId) {
        sceneRepository.findByIdAndNotDeleted(targetSceneId)
                .orElseThrow(() -> new HotspotException(HotspotErrorCode.HOTSPOT_TARGET_SCENE_NOT_FOUND));
    }
}
