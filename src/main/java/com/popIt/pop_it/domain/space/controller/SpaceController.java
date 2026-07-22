package com.popIt.pop_it.domain.space.controller;

import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.dto.SpaceResDTO;
import com.popIt.pop_it.domain.space.exception.SpaceSuccessCode;
import com.popIt.pop_it.domain.space.service.SpaceService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "공간")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    @Operation(
            summary = "공간 등록",
            description = """
                    호스트가 새로운 공간을 등록합니다.
                    - Authorize에 로그인으로 발급받은 Access Token을 입력하세요.
                    - 호스트 등록(POST /api/v1/hosts)을 완료한 사용자만 호출할 수 있습니다. (미등록 시 403)
                    - imageUrls: presigned URL 발급(POST /uploads/presigned-url)으로 S3에 직접 업로드한 뒤 받은 fileUrl 목록입니다.
                      배열 순서가 그대로 노출 순서가 되며, 첫 번째 이미지가 대표 이미지입니다.
                    - facilityIds: 전체 시설 목록 조회(GET /api/v1/facilities) 응답의 facilityId 목록입니다. 선택 안 하면 생략 가능합니다.
                    - latitude/longitude: 프론트에서 카카오 지도 SDK geocoder로 주소를 변환해 전송합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "공간 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 입력값 누락/형식 오류, 계약 가능 기간 오류, 존재하지 않는 시설 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "호스트 프로필 미등록",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SpaceResDTO.CreateResult>> createSpace(
            @AuthenticationPrincipal  AuthUser authUser,
            @Valid @RequestBody SpaceReqDTO.Create request
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        SpaceResDTO.CreateResult result = spaceService.createSpace(userId, request);
        return ResponseEntity.status(SpaceSuccessCode.SPACE_CREATED.getStatus())
                .body(ApiResponse.onSuccess(SpaceSuccessCode.SPACE_CREATED, result));
    }

    // 공간 상세 조회
    @Operation(
            summary = "공간 상세 조회",
            description = """
                    공간 상세 페이지에 필요한 정보를 조회합니다.
                    - 비로그인 상태에서도 호출할 수 있습니다.
                    - 로그인 상태로 호출하면 isMine, isWishlisted가 실제 값으로 채워지고,
                      비로그인 상태에서는 두 값 모두 항상 false로 내려갑니다.
                    - isMine: 요청자가 이 공간을 등록한 호스트인지 여부. 프론트는 이 값으로 게스트/호스트 화면을 분기합니다.
                    - wishCount: 로그인 여부와 무관하게 항상 해당 공간의 총 찜 수입니다.
                    - imageUrls: 등록 시 저장한 노출 순서대로 내려가며, 첫 번째가 대표 이미지입니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "공간 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않거나 삭제된 공간",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping("/{spaceId}")
    public ApiResponse<SpaceResDTO.Detail> getSpaceDetail(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "공간 ID", example = "10")
            @PathVariable Long spaceId
    ) {
        Long userId = (authUser != null && authUser.getUser() != null) ? authUser.getUser().getUserId() : null;

        SpaceResDTO.Detail result = spaceService.getSpaceDetail(userId, spaceId);
        return ApiResponse.onSuccess(SpaceSuccessCode.SPACE_DETAIL_FETCHED, result);
    }

    // @TODO: AI 맞춤 추천 공간 조회
    @Operation(summary = "AI 맞춤 추천 공간 조회", description = "사용자의 찜/이용 이력을 바탕으로 AI가 추천하는 공간 목록을 조회합니다.<br>"
            + "커서 기반 무한스크롤 방식입니다. (cursor 미전달 시 첫 페이지)")
    @GetMapping("/ai-recommended")
    public ApiResponse<SpaceResDTO.AiRecommendedSpaceList> getAiRecommendedSpaces(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        BaseSuccessCode code = SpaceSuccessCode.AI_RECOMMENDED_SPACE_LIST;

        SpaceResDTO.AiRecommendedSpace space = SpaceResDTO.AiRecommendedSpace.builder()
                .spaceId(15L)
                .buildingName("홍대 팝업 스튜디오")
                .tag("이전에 찜한 공간과 비슷해요")
                .district("마포구")
                .roadAddress("서울특별시 마포구 홍익로 123")
                .exclusiveArea(50.0)
                .basicInfo("POPUP")
                .pricePerDay(700000)
                .pricePerWeek(4200000)
                .pricePerMonth(15000000)
                .thumbnailUrl("https://s3.amazonaws.com/popIt/img5.jpg")
                .parkingAvailable(false)
                .isWishlisted(false)
                .build();

        SpaceResDTO.AiRecommendedSpaceList result = SpaceResDTO.AiRecommendedSpaceList.builder()
                .spaces(List.of(space))
                .hasNext(false)
                .nextCursor(null)
                .build();

        return ApiResponse.onSuccess(code, result);
    }
}
