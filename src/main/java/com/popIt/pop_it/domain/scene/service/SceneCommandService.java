package com.popIt.pop_it.domain.scene.service;

import com.popIt.pop_it.domain.hotspot.repository.HotspotRepository;
import com.popIt.pop_it.domain.scene.converter.SceneConverter;
import com.popIt.pop_it.domain.scene.dto.SceneReqDTO;
import com.popIt.pop_it.domain.scene.dto.SceneResDTO;
import com.popIt.pop_it.domain.scene.entity.Scene;
import com.popIt.pop_it.domain.scene.entity.SceneImage;
import com.popIt.pop_it.domain.scene.exception.SceneException;
import com.popIt.pop_it.domain.scene.exception.code.SceneErrorCode;
import com.popIt.pop_it.domain.scene.repository.SceneImageRepository;
import com.popIt.pop_it.domain.scene.repository.SceneRepository;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class SceneCommandService {

    private final SceneRepository sceneRepository;
    private final SceneImageRepository sceneImageRepository;
    private final SpaceRepository spaceRepository;
    private final HotspotRepository hotspotRepository;

    //씬 생성 (사전 제작된 모델 연결)
    public SceneResDTO.SceneIdRes createScene(Long spaceId, Long hostId, SceneReqDTO.SceneCreateReq request) {
        Space space;
        try {
            space = spaceRepository.findByIdAndDeletedAtIsNullForUpdate(spaceId)
                    .orElseThrow(() -> new SceneException(SpaceErrorCode.SPACE_NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            throw new SceneException(SceneErrorCode.SCENE_DEFAULT_ASSIGNMENT_CONFLICT);
        }

        validateHost(space, hostId);

        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        if (isDefault) {
            unmarkExistingDefault(spaceId);
        }

        Scene scene = Scene.builder()
                .space(space)
                .name(request.name())
                .modelUrl(request.modelUrl())
                .thumbnail(request.thumbnail())
                .isDefault(isDefault)
                .cameraPositionX(request.cameraPositionX())
                .cameraPositionY(request.cameraPositionY())
                .cameraPositionZ(request.cameraPositionZ())
                .cameraTargetX(request.cameraTargetX())
                .cameraTargetY(request.cameraTargetY())
                .cameraTargetZ(request.cameraTargetZ())
                .minDistance(request.minDistance())
                .maxDistance(request.maxDistance())
                .minPolarAngle(request.minPolarAngle())
                .maxPolarAngle(request.maxPolarAngle())
                .build();
        sceneRepository.save(scene);

        saveSceneImages(scene, request.imageUrls(), 0);

        return SceneConverter.toSceneId(scene);
    }

    //방(Scene) 사진 등록 - 프론트가 presigned URL로 이미 업로드 완료한 URL 목록을 그대로 저장
    public SceneResDTO.SceneImageUploadRes uploadSceneImages(Long spaceId, Long sceneId, Long hostId, List<String> imageUrls) {
        Scene scene = sceneRepository.findByIdForUpdate(sceneId)
                .orElseThrow(() -> new SceneException(SceneErrorCode.SCENE_NOT_FOUND));

        validateSpaceMatch(scene, spaceId);
        validateHost(scene.getSpace(), hostId);

        int nextSortOrder = sceneImageRepository.findNextSortOrder(sceneId);
        saveSceneImages(scene, imageUrls, nextSortOrder);

        return SceneConverter.toImageUploadResult(sceneId, imageUrls);
    }

    //씬 부분 수정
    public SceneResDTO.SceneIdRes updateScene(Long sceneId, Long hostId, SceneReqDTO.SceneUpdateReq request) {
        Scene scene = sceneRepository.findByIdAndNotDeleted(sceneId)
                .orElseThrow(() -> new SceneException(SceneErrorCode.SCENE_NOT_FOUND));

        validateHost(scene.getSpace(), hostId);

        scene.update(request.name(), request.modelUrl(), request.thumbnail());
        scene.updateCamera(
                request.cameraPositionX(), request.cameraPositionY(), request.cameraPositionZ(),
                request.cameraTargetX(), request.cameraTargetY(), request.cameraTargetZ(),
                request.minDistance(), request.maxDistance(),
                request.minPolarAngle(), request.maxPolarAngle()
        );

        if (Boolean.TRUE.equals(request.isDefault()) && !scene.getIsDefault()) {
            lockSpaceForDefaultAssignment(scene.getSpace().getId());
            unmarkExistingDefault(scene.getSpace().getId());
            scene.markAsDefault();
        } else if (Boolean.FALSE.equals(request.isDefault()) && scene.getIsDefault()) {
            scene.unmarkAsDefault();
        }

        return SceneConverter.toSceneId(scene);
    }

    //씬 삭제 (soft delete)
    //핫스팟의 targetSceneId 참조 검증(HotspotService.validateTargetSceneExists)과 같은 findByIdForUpdate를 써서
    //"이 씬을 가리키는 핫스팟 생성"과 "이 씬 삭제"가 같은 row 락으로 직렬화되게 함
    public void deleteScene(Long sceneId, Long hostId) {
        Scene scene = sceneRepository.findByIdForUpdate(sceneId)
                .orElseThrow(() -> new SceneException(SceneErrorCode.SCENE_NOT_FOUND));

        validateHost(scene.getSpace(), hostId);

        if (hotspotRepository.existsByTargetSceneId(sceneId)) {
            throw new SceneException(SceneErrorCode.SCENE_REFERENCED_BY_HOTSPOT);
        }

        scene.markDeleted();
    }

    //공간의 기존 기본 씬이 있으면 해제
    private void unmarkExistingDefault(Long spaceId) {
        sceneRepository.findDefaultBySpaceId(spaceId)
                .ifPresent(Scene::unmarkAsDefault);
    }

    //기본 씬 재지정 구간(기존 해제~새로 지정) 동안 공간 단위로 락을 걸어 직렬화
    private void lockSpaceForDefaultAssignment(Long spaceId) {
        try {
            spaceRepository.findByIdAndDeletedAtIsNullForUpdate(spaceId)
                    .orElseThrow(() -> new SceneException(SpaceErrorCode.SPACE_NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            throw new SceneException(SceneErrorCode.SCENE_DEFAULT_ASSIGNMENT_CONFLICT);
        }
    }

    //이미지 URL 목록을 startOrder부터 이어서 SceneImage로 저장
    private void saveSceneImages(Scene scene, List<String> imageUrls, int startOrder) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        List<SceneImage> images = IntStream.range(0, imageUrls.size())
                .mapToObj(i -> SceneImage.builder()
                        .scene(scene)
                        .imageUrl(imageUrls.get(i))
                        .sortOrder(startOrder + i)
                        .build())
                .toList();
        sceneImageRepository.saveAll(images);
    }

    //본인 소유 공간인지 확인
    private void validateHost(Space space, Long hostId) {
        if (!space.getHostId().equals(hostId)) {
            throw new SceneException(SceneErrorCode.SCENE_ACCESS_DENIED);
        }
    }

    //경로의 spaceId와 씬이 실제로 속한 공간이 일치하는지 확인 (다른 공간 경로로 씬에 접근하는 것 방지)
    private void validateSpaceMatch(Scene scene, Long spaceId) {
        if (!scene.getSpace().getId().equals(spaceId)) {
            throw new SceneException(SceneErrorCode.SCENE_NOT_FOUND);
        }
    }
}
