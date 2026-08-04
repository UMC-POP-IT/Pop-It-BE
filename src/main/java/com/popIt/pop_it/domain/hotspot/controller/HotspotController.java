package com.popIt.pop_it.domain.hotspot.controller;

import com.popIt.pop_it.domain.hotspot.dto.HotspotReqDTO;
import com.popIt.pop_it.domain.hotspot.dto.HotspotResDTO;
import com.popIt.pop_it.domain.hotspot.exception.code.HotspotSuccessCode;
import com.popIt.pop_it.domain.hotspot.service.HotspotService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공간 3D 큐레이션 - 핫스팟", description = "씬 위 핫스팟(정보/이동 마커) 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HotspotController {

    private final HotspotService hotspotService;

    @Operation(summary = "핫스팟 생성", description = "씬 위에 핫스팟을 추가합니다. type이 INFO면 description 필수, LINK면 targetSceneId 필수입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "핫스팟 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 입력값 누락/형식 오류, type에 맞는 필드 불일치(INFO→description, LINK→targetSceneId), targetSceneId가 핫스팟이 속한 씬과 동일함 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 소유 공간이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "씬을 찾을 수 없음, targetSceneId에 해당하는 씬을 찾을 수 없음 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/scenes/{sceneId}/hotspots")
    public ApiResponse<HotspotResDTO.HotspotIdRes> createHotspot(
            @PathVariable Long sceneId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HotspotReqDTO.HotspotCreateReq request
    ) {
        return ApiResponse.onSuccess(
                HotspotSuccessCode.HOTSPOT_CREATED,
                hotspotService.createHotspot(sceneId, authUser.getUser().getUserId(), request)
        );
    }

    @Operation(summary = "핫스팟 수정", description = "핫스팟을 부분 수정합니다. type은 생성 후 변경할 수 없고, 기존 type과 안 맞는 필드(예: INFO인데 targetSceneId)를 보내면 400이 발생합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "핫스팟 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "형식 오류(공백만 입력), 기존 type에 맞지 않는 필드(INFO인데 targetSceneId, LINK인데 description), targetSceneId가 핫스팟이 속한 씬과 동일함 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 소유 공간이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "핫스팟을 찾을 수 없음, targetSceneId에 해당하는 씬을 찾을 수 없음 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PatchMapping("/hotspots/{hotspotId}")
    public ApiResponse<HotspotResDTO.HotspotIdRes> updateHotspot(
            @PathVariable Long hotspotId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HotspotReqDTO.HotspotUpdateReq request
    ) {
        return ApiResponse.onSuccess(
                HotspotSuccessCode.HOTSPOT_UPDATED,
                hotspotService.updateHotspot(hotspotId, authUser.getUser().getUserId(), request)
        );
    }

    @Operation(summary = "핫스팟 삭제", description = "핫스팟을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "핫스팟 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 소유 공간이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "핫스팟을 찾을 수 없음",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @DeleteMapping("/hotspots/{hotspotId}")
    public ApiResponse<Void> deleteHotspot(
            @PathVariable Long hotspotId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hotspotService.deleteHotspot(hotspotId, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(HotspotSuccessCode.HOTSPOT_DELETED, null);
    }
}
