package com.popIt.pop_it.domain.wishlist.controller;

import com.popIt.pop_it.domain.wishlist.dto.WishlistResDTO;
import com.popIt.pop_it.domain.wishlist.exception.code.WishlistSuccessCode;
import com.popIt.pop_it.domain.wishlist.service.WishlistService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공간 찜", description = "찜 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/wishlist")
public class UserWishlistController {

    private final WishlistService wishlistService;

    @Operation(
            summary = "내가 찜한 공간 목록 조회",
            description = """
                    로그인한 사용자가 찜한 공간 목록을 최근 찜한 순으로 페이징 조회합니다.
                    - 인증 필요: 우측 상단 Authorize에 Access Token을 입력하세요.
                    - 삭제된 공간은 목록에서 제외됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 페이징 파라미터(page<0 또는 size 범위 밖)", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping
    public ApiResponse<WishlistResDTO.WishlistListRes> getMyWishlist(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1~12)", example = "12")
            @RequestParam(defaultValue = "12") @Min(1) @Max(12) int size
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        WishlistResDTO.WishlistListRes result = wishlistService.getMyWishlist(userId, page, size);

        return ApiResponse.onSuccess(WishlistSuccessCode.WISH_LIST, result);
    }
}
