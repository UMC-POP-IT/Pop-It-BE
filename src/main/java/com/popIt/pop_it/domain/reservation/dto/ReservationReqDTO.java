package com.popIt.pop_it.domain.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationReqDTO {

    public record ReservationCreateReq(
            @Schema(description = "예약할 공간 ID", example = "1")
            @NotNull Long spaceId,

            @Schema(description = "이용 시작일 (오늘 이후, yyyy-MM-dd)", example = "2026-09-01")
            @NotNull LocalDate startDate,

            @Schema(description = "이용 종료일 (시작일 이후, 최대 90일 이내, yyyy-MM-dd)", example = "2026-09-05")
            @NotNull LocalDate endDate,

            @Schema(description = "이용 목적", example = "팝업스토어 운영")
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
            @Schema(description = "퇴실 증빙 사진 URL 목록. presigned URL로 S3에 이미 업로드 완료한 URL만 전달 (최소 1장, 여러 장 가능)")
            @NotEmpty List<@NotBlank String> imageUrls // 여러 장 업로드 가능, 최소 한 장 필요
    ) {
    }
}
