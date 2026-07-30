package com.popIt.pop_it.domain.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class ReservationReqDTO {

    public record ReservationCreateReq(
            @NotNull Long spaceId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank String usagePurpose
    ) {
    }

    public record ReservationCheckoutReq(
            @NotEmpty List<@NotBlank String> photoUrls // 여러 장 업로드 가능, 최소 한 장 필요
    ) {
    }
}
