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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공간 찜", description = "찜 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/spaces/{spaceId}/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(
            summary = "찜 토글",
            description = """
                    공간에 대한 찜 상태를 토글합니다.
                    - 인증 필요: 우측 상단 Authorize에 Access Token을 입력하세요.
                    - 이미 찜한 공간이면 해제(isWishlisted=false), 아니면 등록(isWishlisted=true)합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "찜 등록/해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 공간", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping
    public ApiResponse<WishlistResDTO.WishlistToggleRes> toggleWishlist(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "공간 ID", example = "10")
            @PathVariable Long spaceId
    ) {
        // Security 오류 등으로 인증 주체가 비어있을 경우 NPE(500) 대신 표준 401로 처리
        if (authUser == null || authUser.getUser() == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }

        Long userId = authUser.getUser().getUserId();
        WishlistResDTO.WishlistToggleRes result = wishlistService.toggle(userId, spaceId);

        // 토글 결과에 따라 등록/해제 메시지를 구분해 응답
        WishlistSuccessCode code = result.isWishlisted()
                ? WishlistSuccessCode.WISH_ADDED
                : WishlistSuccessCode.WISH_REMOVED;
        return ApiResponse.onSuccess(code, result);
    }
}
