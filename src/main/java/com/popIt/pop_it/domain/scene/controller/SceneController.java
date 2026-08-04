package com.popIt.pop_it.domain.scene.controller;

import com.popIt.pop_it.domain.scene.dto.SceneReqDTO;
import com.popIt.pop_it.domain.scene.dto.SceneResDTO;
import com.popIt.pop_it.domain.scene.exception.code.SceneSuccessCode;
import com.popIt.pop_it.domain.scene.service.SceneCommandService;
import com.popIt.pop_it.domain.scene.service.SceneQueryService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공간 3D 큐레이션 - 씬", description = "3D 모델 기반 씬(공간 뷰) 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SceneController {

    private final SceneQueryService sceneQueryService;
    private final SceneCommandService sceneCommandService;

    @Operation(summary = "씬 목록 조회", description = "공간에 등록된 씬 목록을 조회합니다. (기본 씬 여부는 각 씬의 isDefault로 표시)")
    @GetMapping("/spaces/{spaceId}/scenes")
    public ApiResponse<SceneResDTO.SceneListRes> getScenes(
            @PathVariable Long spaceId
    ) {
        return ApiResponse.onSuccess(
                SceneSuccessCode.SCENE_LIST,
                sceneQueryService.getScenes(spaceId)
        );
    }

    @Operation(summary = "씬 상세 조회", description = "모델 URL, 카메라 초기 설정, 핫스팟 목록을 조회합니다.")
    @GetMapping("/scenes/{sceneId}")
    public ApiResponse<SceneResDTO.SceneDetailRes> getSceneDetail(
            @PathVariable Long sceneId
    ) {
        return ApiResponse.onSuccess(
                SceneSuccessCode.SCENE_DETAIL,
                sceneQueryService.getSceneDetail(sceneId)
        );
    }

    @Operation(summary = "씬 생성", description = "사전 제작된 3D 모델(.glb)을 씬에 연결하여 생성합니다.<br>"
            + "사진은 presigned URL로 먼저 S3에 올린 뒤, 완료된 URL만 imageUrls로 전달합니다.")
    @PostMapping("/spaces/{spaceId}/scenes")
    public ApiResponse<SceneResDTO.SceneIdRes> createScene(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SceneReqDTO.SceneCreateReq request
    ) {
        return ApiResponse.onSuccess(
                SceneSuccessCode.SCENE_CREATED,
                sceneCommandService.createScene(spaceId, authUser.getUser().getUserId(), request)
        );
    }

    @Operation(summary = "씬 사진 등록", description = "프론트가 presigned URL로 이미 S3에 업로드 완료한 이미지 URL 목록을 받아 씬에 등록합니다. (기존 사진 뒤에 이어붙음)")
    @PostMapping("/spaces/{spaceId}/scenes/{sceneId}/images")
    public ApiResponse<SceneResDTO.ImageUploadResultRes> uploadSceneImages(
            @PathVariable Long spaceId,
            @PathVariable Long sceneId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SceneReqDTO.SceneImageUploadReq request
    ) {
        return ApiResponse.onSuccess(
                SceneSuccessCode.SCENE_IMAGES_UPLOADED,
                sceneCommandService.uploadSceneImages(spaceId, sceneId, authUser.getUser().getUserId(), request.imageUrls())
        );
    }

    @Operation(summary = "씬 수정", description = "씬 이름/모델/썸네일/기본 씬 여부를 부분 수정합니다. (null인 필드는 기존 값 유지)")
    @PatchMapping("/scenes/{sceneId}")
    public ApiResponse<SceneResDTO.SceneIdRes> updateScene(
            @PathVariable Long sceneId,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody SceneReqDTO.SceneUpdateReq request
    ) {
        return ApiResponse.onSuccess(
                SceneSuccessCode.SCENE_UPDATED,
                sceneCommandService.updateScene(sceneId, authUser.getUser().getUserId(), request)
        );
    }

    @Operation(summary = "씬 삭제", description = "씬을 삭제합니다. (soft delete)<br>"
            + "다른 씬의 링크 핫스팟이 참조 중이면 삭제할 수 없습니다.")
    @DeleteMapping("/scenes/{sceneId}")
    public ApiResponse<Void> deleteScene(
            @PathVariable Long sceneId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        sceneCommandService.deleteScene(sceneId, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(SceneSuccessCode.SCENE_DELETED, null);
    }
}
