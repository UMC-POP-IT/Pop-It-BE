package com.popIt.pop_it.domain.reservation.dto;

import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReservationResDTO {

    @Builder
    public record ReservationSummaryRes(
            // 목록 조회용 (게스트: /me, 호스트: /host)
            Long reservationId,
            ReservationStatus status,
            String statusDescription,
            LocalDate startDate,
            LocalDate endDate,
            String usagePurpose, // 호스트 목록에 노출
            Long totalPrice,
            Boolean isPhotoVerified, // true: 인증 완료, false: 인증 필요
            ReservationSpaceSummaryRes space,
            // 호스트 목록에서만 필요 (게스트 목록에선 null)
            ReservationGuestSummaryRes guest
    ) {
    }

    @Builder
    public record ReservationPagedSummaryRes(
            List<ReservationSummaryRes> reservations,
            Boolean hasNext,
            String nextCursor
    ) {
    }

    @Builder
    public record ReservationStatusCountsRes(
            Map<ReservationStatus, Long> countsByStatus
    ) {
    }

    @Builder
    public record ReservationSpaceSummaryRes(
            Long spaceId,
            String buildingName,
            String address,
            String thumbnailUrl // 대표 사진(sortOrder 최솟값)
    ) {
    }

    @Builder
    public record ReservationGuestSummaryRes(
            Long userId,
            String nickname
    ) {
    }

    @Builder
    public record ReservationCreateRes(
            Long reservationId,
            ReservationStatus status,
            String statusDescription,
            Long rentalFee,
            Long deposit,
            Long insuranceFee,
            Long totalPrice
    ) {
    }

    @Builder
    public record ReservationStatusChangeRes(
            Long reservationId,
            ReservationStatus status,
            String statusDescription
    ) {
    }

    @Builder
    public record ReservationUnavailableDatesRes(
            List<ReservationDateRangeRes> unavailableDates
    ) {
    }

    @Builder
    public record ReservationDateRangeRes(
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    @Builder
    public record ReservationCheckoutImagesRes(
            List<String> photoUrls
    ) {
    }

}

