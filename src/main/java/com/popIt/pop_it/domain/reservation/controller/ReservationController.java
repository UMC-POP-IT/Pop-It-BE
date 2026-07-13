package com.popIt.pop_it.domain.reservation.controller;

import com.popIt.pop_it.domain.reservation.dto.ReservationReqDTO;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationSuccessCode;
import com.popIt.pop_it.domain.reservation.service.ReservationCommandService;
import com.popIt.pop_it.domain.reservation.service.ReservationQueryService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationQueryService reservationQueryService;
    private final ReservationCommandService reservationCommandService;

    //게스트 예약 목록 조회
    @GetMapping("/me")
    public ApiResponse<List<ReservationResDTO.Summary>> getMyReservations(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getMyReservations(userId)
        );
    }

    //호스트 예약 목록 조회
    @GetMapping("/host")
    public ApiResponse<List<ReservationResDTO.Summary>> getHostReservations(
            @AuthenticationPrincipal Long hostId
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getHostReservations(hostId)
        );
    }

    //예약 요청
    @PostMapping
    public ApiResponse<ReservationResDTO.Create> createReservation(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReservationReqDTO.Create request
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_REQUEST,
                reservationCommandService.createReservation(userId, request)
        );
    }

    //예약 승인(호스트)
    @PostMapping("/{reservationId}/approve")
    public ApiResponse<ReservationResDTO.StatusChange> approveReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Long hostId
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_OK,
                reservationCommandService.approveReservation(reservationId, hostId)
        );
    }

    //예약 거절(호스트)
    @PostMapping("/{reservationId}/reject")
    public ApiResponse<ReservationResDTO.StatusChange> rejectReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Long hostId
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_NO,
                reservationCommandService.rejectReservation(reservationId, hostId)
        );
    }

}
