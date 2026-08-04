package com.popIt.pop_it.domain.reservation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationReqDTO {

    public record ReservationCreateReq(
            @NotNull Long spaceId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank String usagePurpose
    ) {
        private static final int MAX_RESERVATION_DAYS = 90;

        @AssertTrue(message = "예약 시작일이 종료일보다 늦을 수 없습니다.")
        private boolean isValidPeriodOrder() {
            if (startDate == null || endDate == null) return true;
            return !startDate.isAfter(endDate);
        }

        @AssertTrue(message = "예약 가능 기간은 최대 90일입니다.")
        private boolean isWithinMaxDuration() {
            if (startDate == null || endDate == null) return true;
            return ChronoUnit.DAYS.between(startDate, endDate) + 1 <= MAX_RESERVATION_DAYS;
        }
    }

    public record ReservationCheckoutReq(
            @NotEmpty List<@NotBlank String> photoUrls // 여러 장 업로드 가능, 최소 한 장 필요
    ) {
    }
}
