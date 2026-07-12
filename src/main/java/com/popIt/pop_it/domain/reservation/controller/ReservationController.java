package com.popIt.pop_it.domain.reservation.controller;

import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationSuccessCode;
import com.popIt.pop_it.domain.reservation.service.ReservationQueryService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationQueryService reservationQueryService;

    @GetMapping("/me")
    public ApiResponse<List<ReservationResDTO.Summary>> getMyReservations(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getMyReservations(userId)
        );
    }

    @GetMapping("/host")
    public ApiResponse<List<ReservationResDTO.Summary>> getHostReservations(
            @AuthenticationPrincipal Long hostId
    ) {
        return ApiResponse.onSuccess(
                ReservationSuccessCode.RESERVATION_LIST,
                reservationQueryService.getHostReservations(hostId)
        );
    }
}
