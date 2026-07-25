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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

    @Operation(
            summary = "내 공간 목록 조회",
            description = """
                    호스트모드 - 내 공간 페이지. 로그인한 호스트 본인이 등록한 공간 목록을 조회합니다.
                    - Authorize에 로그인으로 발급받은 Access Token을 입력하세요.
                    - 호스트 등록을 완료한 사용자만 호출할 수 있습니다.
                    - 등록일 최신순으로 정렬되며, 삭제된 공간은 제외됩니다.
                    - thumbnailUrl: 등록 시 저장한 사진 목록의 첫 번째(대표) 이미지입니다.
                    - page는 0부터 시작합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "내 공간 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "호스트 프로필 미등록",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping("/my")
    public ApiResponse<SpaceResDTO.MyListResult> getMySpaces(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기", example = "4")
            @RequestParam(defaultValue = "4") @Min(1) @Max(10) int size
    ) {
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        SpaceResDTO.MyListResult result = spaceService.getMySpaces(userId, page, size);
        return ApiResponse.onSuccess(SpaceSuccessCode.MY_PAGE_LIST_FETCHED, result);
    }

    @Operation(
            summary = "공간 탐색 (검색/필터)",
            description = """
                    게스트 메인 화면의 공간 탐색 목록입니다. 통합 검색 + 필터 + 페이지네이션을 지원합니다.
                    - 비로그인 상태에서도 호출할 수 있습니다.
                    - 로그인 상태로 호출하면 isWishlisted가 실제 값으로 채워지고, 비로그인 상태에서는 항상 false입니다.
                    - wishCount는 로그인 여부와 무관하게 항상 해당 공간의 총 찜 수입니다.
                    - 모든 파라미터는 생략 가능하며, 생략하면 해당 조건은 적용되지 않습니다.
                    
                    [keyword] 통합 검색창에 대응합니다. 아래를 한 번에 부분 일치로 검색합니다.
                      - 공간: 건물명
                      - 지역 이름: 구 / 동 / 도로명 주소
                      - 정보: 공간 용도(팝업스토어, 전시/갤러리 ...), 공간 구조 유형(오픈형 홀, 가벽 분리형 ...)
                        한글 이름으로 검색하며 공백은 무시합니다. ("오픈형 홀" = "오픈형홀")
                    
                    [spaceCategory] 용도 필터 드롭다운에 대응합니다. '전체'는 파라미터를 생략하면 됩니다.
                    [district] 지역 필터 드롭다운에 대응합니다. 서울 25개 구 이름을 그대로 전달하세요.
                    
                    - keywords는 카드 하단 태그입니다. (#지역명(동), #공간유형) 순서이며,
                      동 정보가 없는 공간은 공간유형 하나만 내려갑니다.
                    - 정렬은 등록일 최신순이며, 삭제된 공간은 제외됩니다.
                    - page는 0부터 시작하고, totalCount로 페이지네이션 버튼 수를 계산하세요.
                      size 기본값은 28입니다. (그리드 4x7 = 한 페이지당 28개)
                    """

    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "공간 탐색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (page/size 범위 초과, 정의되지 않은 spaceCategory 값 등)",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping
    public ApiResponse<SpaceResDTO.SpaceSearchListRes> searchSpaces(
            @AuthenticationPrincipal AuthUser authUser,
            @ParameterObject @Valid @ModelAttribute SpaceReqDTO.SpaceSearchReq request
    ) {
        Long userId = (authUser != null && authUser.getUser() != null) ? authUser.getUser().getUserId() : null;

        SpaceResDTO.SpaceSearchListRes result = spaceService.searchSpaces(userId, request);
        return ApiResponse.onSuccess(SpaceSuccessCode.SPACE_SEARCH_FETCHED, result);
    }

    @Operation(
            summary = "공간 수정",
            description = "호스트모드 - 등록한 공간의 정보를 수정합니다. 등록 시 입력한 모든 항목을 수정할 수 있습니다. ",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "공간 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "형식 오류/잘못된 enum 값, 계약 가능 기간 오류, 위도·경도 미쌍, 존재하지 않는 시설 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인이 등록한 공간이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않거나 이미 삭제된 공간",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PatchMapping("/{spaceId}")
    public ApiResponse<SpaceResDTO.UpdateResult> updateSpace(
            @AuthenticationPrincipal AuthUser authuser,
            @Parameter(description = "공간 ID", example = "10")
            @PathVariable Long spaceId,
            @Valid @RequestBody SpaceReqDTO.Update request
    ) {
        if (authuser == null || authuser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authuser.getUser().getUserId();
        SpaceResDTO.UpdateResult result = spaceService.updateSpace(userId, spaceId, request);
        return ApiResponse.onSuccess(SpaceSuccessCode.SPACE_UPDATED, result);
    }

    @Operation(
            summary = "공간 삭제",
            description = "호스트모드 - 등록한 공간을 삭제합니다. (소프트 삭제)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "공간 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "진행 중인 예약이 있어 삭제 불가",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인이 등록한 공간이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않거나 이미 삭제된 공간",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @DeleteMapping("/{spaceId}")
    public ApiResponse<SpaceResDTO.DeleteResult> deleteSpace(
            @AuthenticationPrincipal AuthUser authuser,
            @Parameter(description = "공간 ID", example = "10")
            @PathVariable Long spaceId
    ) {
        if (authuser == null || authuser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authuser.getUser().getUserId();
        SpaceResDTO.DeleteResult result = spaceService.deleteSpace(userId, spaceId);
        return ApiResponse.onSuccess(SpaceSuccessCode.SPACE_DELETED, result);
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
