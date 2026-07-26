package com.popIt.pop_it.domain.hotspot.controller;

import com.popIt.pop_it.domain.hotspot.dto.HotspotReqDTO;
import com.popIt.pop_it.domain.hotspot.dto.HotspotResDTO;
import com.popIt.pop_it.domain.hotspot.exception.code.HotspotSuccessCode;
import com.popIt.pop_it.domain.hotspot.service.HotspotService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공간 3D 큐레이션 - 핫스팟")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HotspotController {

    private final HotspotService hotspotService;

    @Operation(summary = "핫스팟 생성", description = "씬 위에 핫스팟을 추가합니다. type이 INFO면 description 필수, LINK면 targetSceneId 필수입니다.")
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
    @PatchMapping("/hotspots/{hotspotId}")
    public ApiResponse<HotspotResDTO.HotspotIdRes> updateHotspot(
            @PathVariable Long hotspotId,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody HotspotReqDTO.HotspotUpdateReq request
    ) {
        return ApiResponse.onSuccess(
                HotspotSuccessCode.HOTSPOT_UPDATED,
                hotspotService.updateHotspot(hotspotId, authUser.getUser().getUserId(), request)
        );
    }

    @Operation(summary = "핫스팟 삭제", description = "핫스팟을 삭제합니다.")
    @DeleteMapping("/hotspots/{hotspotId}")
    public ApiResponse<Void> deleteHotspot(
            @PathVariable Long hotspotId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hotspotService.deleteHotspot(hotspotId, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(HotspotSuccessCode.HOTSPOT_DELETED, null);
    }
}
