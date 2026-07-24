package com.popIt.pop_it.domain.scene.service;

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

    //씬 생성 (사전 제작된 모델 연결)
    public SceneResDTO.SceneId createScene(Long spaceId, Long hostId, SceneReqDTO.Create request) {
        Space space = spaceRepository.findByIdAndDeletedAtIsNull(spaceId)
                .orElseThrow(() -> new SceneException(SpaceErrorCode.SPACE_NOT_FOUND));

        validateHost(space, hostId);

        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        if (isDefault) {
            lockSpaceForDefaultAssignment(spaceId);
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
    //비관적 락으로 sortOrder 조회~저장 구간을 직렬화 (동시 등록 시 순번 중복 방지, 즉시실패 없이 순차 대기)
    public SceneResDTO.ImageUploadResult uploadSceneImages(Long spaceId, Long sceneId, Long hostId, List<String> imageUrls) {
        Scene scene = sceneRepository.findByIdForUpdate(sceneId)
                .orElseThrow(() -> new SceneException(SceneErrorCode.SCENE_NOT_FOUND));

        validateSpaceMatch(scene, spaceId);
        validateHost(scene.getSpace(), hostId);

        int nextSortOrder = sceneImageRepository.findNextSortOrder(sceneId);
        saveSceneImages(scene, imageUrls, nextSortOrder);

        return SceneConverter.toImageUploadResult(sceneId, imageUrls);
    }

    //씬 부분 수정
    public SceneResDTO.SceneId updateScene(Long sceneId, Long hostId, SceneReqDTO.Update request) {
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
    public void deleteScene(Long sceneId, Long hostId) {
        Scene scene = sceneRepository.findByIdAndNotDeleted(sceneId)
                .orElseThrow(() -> new SceneException(SceneErrorCode.SCENE_NOT_FOUND));

        validateHost(scene.getSpace(), hostId);

        // TODO: Hotspot 도메인 구현 후 - 다른 씬의 LINK 핫스팟이 이 씬을 targetSceneId로 참조 중이면
        //       SceneErrorCode.SCENE_REFERENCED_BY_HOTSPOT 던지도록 참조 카운트 체크 추가

        scene.markDeleted();
    }

    //공간의 기존 기본 씬이 있으면 해제
    private void unmarkExistingDefault(Long spaceId) {
        sceneRepository.findDefaultBySpaceId(spaceId)
                .ifPresent(Scene::unmarkAsDefault);
    }

    //기본 씬 재지정 구간(기존 해제~새로 지정) 동안 공간 단위로 락을 걸어 직렬화
    //동시에 같은 spaceId로 isDefault=true 요청이 들어와도 하나만 통과, 나머지는 즉시 충돌 응답
    private void lockSpaceForDefaultAssignment(Long spaceId) {
        try {
            spaceRepository.findByIdForUpdate(spaceId)
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
