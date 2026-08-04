package com.popIt.pop_it.domain.reservation.controller;

import com.popIt.pop_it.domain.reservation.dto.ReservationReqDTO;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationSuccessCode;
import com.popIt.pop_it.domain.reservation.service.ReservationCommandService;
import com.popIt.pop_it.domain.reservation.service.ReservationQueryService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Tag(name = "예약", description = "예약 요청/승인/거절/취소 및 퇴실 처리 API")
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationQueryService reservationQueryService;
    private final ReservationCommandService reservationCommandService;

    @Operation(summary = "게스트 예약 목록 조회", description = "게스트 본인의 예약 내역을 조회합니다.<br>"
            + "커서 기반 무한스크롤 방식이며, status로 탭(승인대기/계약대기/사용중 등) 필터링이 가능합니다.<br>"
            + "(status 미전달 시 전체 조회, cursor 미전달 시 첫 페이지)")
    @GetMapping("/me")
    public ApiResponse<ReservationResDTO.ReservationPagedSummaryRes> getMyReservations(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "예약 상태 필터 (미전달 시 전체 조회)", example = "PENDING_APPROVAL")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "페이지네이션 커서. 직전 응답의 nextCursor 값을 그대로 전달 (미전달 시 첫 페이지)",
                    example = "2026-08-04T10:30:00|42")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지당 조회 개수 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getMyReservations(authUser.getUser().getUserId(), status, cursor, size)
        );
    }

    @Operation(summary = "게스트 예약 상태별 개수 조회", description = "게스트 예약관리 화면 상단 탭(승인대기/계약대기/사용중 등)에 붙는 배지 숫자를 상태별로 반환합니다.")
    @GetMapping("/me/status-counts")
    public ApiResponse<ReservationResDTO.ReservationStatusCountsRes> getMyStatusCounts(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getMyStatusCounts(authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "호스트 예약 목록 조회", description = "호스트가 등록한 공간에 걸린 예약 내역을 조회합니다.<br>"
            + "커서 기반 무한스크롤 방식이며, status로 탭 필터링이 가능합니다.<br>"
            + "(status 미전달 시 전체 조회, cursor 미전달 시 첫 페이지)")
    @GetMapping("/host")
    public ApiResponse<ReservationResDTO.ReservationPagedSummaryRes> getHostReservations(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "예약 상태 필터 (미전달 시 전체 조회)", example = "PENDING_APPROVAL")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "페이지네이션 커서. 직전 응답의 nextCursor 값을 그대로 전달 (미전달 시 첫 페이지)",
                    example = "2026-08-04T10:30:00|42")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지당 조회 개수 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getHostReservations(authUser.getUser().getUserId(), status, cursor, size)
        );
    }

    @Operation(summary = "호스트 예약 상태별 개수 조회", description = "호스트 예약관리 화면 상단 탭에 붙는 배지 숫자를 상태별로 반환합니다.")
    @GetMapping("/host/status-counts")
    public ApiResponse<ReservationResDTO.ReservationStatusCountsRes> getHostStatusCounts(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getHostStatusCounts(authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "예약 요청", description = "게스트가 특정 공간에 대해 예약을 요청합니다. (상태: 승인대기 PENDING_APPROVAL 생성)<br>"
            + "대여료/보험료(대여료의 5%)/보증금을 서버에서 계산해 총 결제 금액을 반환합니다.<br>"
            + "(이용 기간 최대 90일, 공간의 대여 가능 기간 범위 내에서만 요청 가능)")
    @PostMapping
    public ApiResponse<ReservationResDTO.ReservationCreateRes> createReservation(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReservationReqDTO.ReservationCreateReq request
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_REQUEST,
                reservationCommandService.createReservation(authUser.getUser().getUserId(), request)
        );
    }

    @Operation(summary = "예약 승인", description = "호스트가 승인대기 상태의 예약을 승인합니다. (PENDING_APPROVAL → APPROVED)<br>"
            + "승인 시 계약(서명대기) 절차로 이어집니다.")
    @PostMapping("/{reservationId}/approve")
    public ApiResponse<ReservationResDTO.ReservationStatusChangeRes> approveReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_OK,
                reservationCommandService.approveReservation(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "예약 거절", description = "호스트가 승인대기 상태의 예약을 거절합니다. (PENDING_APPROVAL → CANCELLED)")
    @PostMapping("/{reservationId}/reject")
    public ApiResponse<ReservationResDTO.ReservationStatusChangeRes> rejectReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_NO,
                reservationCommandService.rejectReservation(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "예약 취소(게스트)", description = "게스트가 본인 예약을 취소합니다.<br>"
            + "승인대기/승인완료(PENDING_APPROVAL, APPROVED) 상태에서만 가능하며, 결제(계약완료) 이후 상태는 이 API로 취소할 수 없습니다.")
    @PostMapping("/{reservationId}/cancel")
    public ApiResponse<ReservationResDTO.ReservationStatusChangeRes> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_GUEST_CANCEL,
                reservationCommandService.cancelByGuest(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "퇴실 증빙 제출", description = "게스트가 이용 완료 후 퇴실 사진을 업로드합니다. (여러 장 등록 가능)<br>"
            + "제출 후에는 호스트 승인 또는 24시간 경과 시 자동 승인으로 퇴실이 완료됩니다.<br>"
            + "(현재는 이용 완료 USAGE_COMPLETED 상태에서만 제출 가능합니다.)")
    @PostMapping("/{reservationId}/checkout")
    public ApiResponse<ReservationResDTO.ReservationStatusChangeRes> submitCheckout(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReservationReqDTO.ReservationCheckoutReq request
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_CHECKOUT_PHOTO,
                reservationCommandService.submitCheckout(reservationId, authUser.getUser().getUserId(), request)
        );
    }

    @Operation(summary = "퇴실 승인", description = "호스트가 제출된 퇴실 증빙을 확인하고 승인합니다. (USAGE_COMPLETED → CHECKOUT_COMPLETED)<br>"
            + "승인 즉시 정산되도록 연동 예정입니다.")
    @PostMapping("/{reservationId}/checkout/approve")
    public ApiResponse<ReservationResDTO.ReservationStatusChangeRes> approveCheckout(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_CHECKOUT_OK,
                reservationCommandService.approveCheckout(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "퇴실 거절", description = "호스트가 제출된 퇴실 증빙을 거절하고 게스트에게 재인증을 요청합니다.<br>"
            + "거절 시 기존 제출 사진은 삭제되지 않고 비활성화되어 게스트가 조회할 수 있으며, 재제출하기 전까지는 다시 거절할 수 없습니다. (재제출 전까지 자동승인 대상에서 제외됩니다.)")
    @PostMapping("/{reservationId}/checkout/reject")
    public ApiResponse<ReservationResDTO.ReservationStatusChangeRes> rejectCheckout(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_CHECKOUT_REJECT,
                reservationCommandService.rejectCheckout(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "퇴실 증빙 사진 조회", description = "호스트가 게스트의 퇴실 증빙 사진 목록을 조회합니다. (승인/거절 전 확인용)")
    @GetMapping("/{reservationId}/checkout-images")
    public ApiResponse<ReservationResDTO.ReservationCheckoutImagesRes> getCheckoutImages(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_CHECKOUT_IMAGES,
                reservationQueryService.getCheckoutImages(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "게스트 퇴실 증빙 사진 조회", description = "게스트 본인이 제출한 퇴실 증빙 사진을 조회합니다.<br>"
            + "거절된 상태(재인증 대기)라면 가장 최근 거절된 사진을, 그 외에는 현재 유효한(승인 대기/완료) 제출 사진을 반환합니다.")
    @GetMapping("/{reservationId}/checkout-photos")
    public ApiResponse<ReservationResDTO.ReservationCheckoutPhotosRes> getCheckoutPhotos(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_CHECKOUT_PHOTOS,
                reservationQueryService.getCheckoutPhotosForGuest(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "퇴실 승인 여부 조회", description = "예약의 퇴실 승인 진행 상태를 조회합니다. 게스트/호스트 모두 본인이 연관된 예약이면 조회 가능합니다.<br>"
            + "status가 CHECKOUT_COMPLETED면 승인 완료, checkoutRejected가 true면 거절(재인증 대기), "
            + "checkoutSubmittedAt만 있고 위 두 조건에 해당하지 않으면 제출 후 호스트 확인 대기 중입니다.")
    @GetMapping("/{reservationId}/checkout-approval")
    public ApiResponse<ReservationResDTO.ReservationCheckoutApprovalRes> getCheckoutApproval(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_CHECKOUT_APPROVAL,
                reservationQueryService.getCheckoutApproval(reservationId, authUser.getUser().getUserId())
        );
    }

    @Operation(summary = "공간별 예약 불가 날짜 조회", description = "특정 공간에 대해 이미 선점(승인대기~진행 중)된 기간 목록을 반환합니다.<br>"
            + "프론트에서 예약 요청 화면의 캘린더에 예약 불가 날짜를 비활성화 표시하는 용도입니다. (지난 날짜는 제외됩니다.)")
    @GetMapping("/{spaceId}/unavailable-dates")
    public ApiResponse<ReservationResDTO.ReservationUnavailableDatesRes> getUnavailableDates(
            @PathVariable Long spaceId
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_UNAVAILABLE_DATES,
                reservationQueryService.getUnavailableDates(spaceId)
        );
    }

}
