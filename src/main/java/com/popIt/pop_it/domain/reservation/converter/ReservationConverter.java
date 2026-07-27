package com.popIt.pop_it.domain.reservation.converter;

import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.CheckoutImage;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.repository.ReservationDateRange;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.user.entity.User;

import java.util.List;

public class ReservationConverter {
    public static ReservationResDTO.ReservationSummaryRes toSummary(
            Reservation reservation, boolean includeGuest, String thumbnailUrl, boolean isPhotoVerified
    ) {
        return ReservationResDTO.ReservationSummaryRes.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus().getDescription())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .usagePurpose(reservation.getUsagePurpose())
                .totalPrice(reservation.getTotalPrice())
                .isPhotoVerified(isPhotoVerified)
                .space(toSpaceSummary(reservation.getSpace(), thumbnailUrl))
                .guest(includeGuest ? toGuestSummary(reservation.getUser()) : null)
                .build();
    }

    private static ReservationResDTO.ReservationSpaceSummaryRes toSpaceSummary(Space space, String thumbnailUrl) {
        return ReservationResDTO.ReservationSpaceSummaryRes.builder()
                .spaceId(space.getId())
                .buildingName(space.getBuildingName())
                .address(space.getRoadAddress())
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private static ReservationResDTO.ReservationGuestSummaryRes toGuestSummary(User user) {
        return ReservationResDTO.ReservationGuestSummaryRes.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .build();
    }

    public static ReservationResDTO.ReservationCreateRes toCreateResult(Reservation reservation) {
        return ReservationResDTO.ReservationCreateRes.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus().getDescription())
                .rentalFee(reservation.getRentalFee())
                .deposit(reservation.getDeposit())
                .insuranceFee(reservation.getInsuranceFee())
                .totalPrice(reservation.getTotalPrice())
                .build();
    }

    public static ReservationResDTO.ReservationStatusChangeRes toStatusChange(Reservation reservation) {
        return ReservationResDTO.ReservationStatusChangeRes.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus().getDescription())
                .build();
    }

    public static ReservationResDTO.ReservationUnavailableDatesRes toUnavailableDates(List<ReservationDateRange> ranges) {
        List<ReservationResDTO.ReservationDateRangeRes> dateRanges = ranges.stream()
                .map(r -> ReservationResDTO.ReservationDateRangeRes.builder()
                        .startDate(r.getStartDate())
                        .endDate(r.getEndDate())
                        .build())
                .toList();

        return ReservationResDTO.ReservationUnavailableDatesRes.builder()
                .unavailableDates(dateRanges)
                .build();
    }

    public static ReservationResDTO.ReservationCheckoutImagesRes toCheckoutImages(List<CheckoutImage> images) {
        List<String> photoUrls = images.stream()
                .map(CheckoutImage::getCheckoutImageUrl)
                .toList();

        return ReservationResDTO.ReservationCheckoutImagesRes.builder()
                .photoUrls(photoUrls)
                .build();
    }

    public static ReservationResDTO.ReservationCheckoutPhotosRes toCheckoutPhotos(
            boolean checkoutRejected, List<CheckoutImage> images
    ) {
        List<String> photoUrls = images.stream()
                .map(CheckoutImage::getCheckoutImageUrl)
                .toList();

        return ReservationResDTO.ReservationCheckoutPhotosRes.builder()
                .checkoutRejected(checkoutRejected)
                .photoUrls(photoUrls)
                .build();
    }

    public static ReservationResDTO.ReservationCheckoutApprovalRes toCheckoutApproval(Reservation reservation) {
        return ReservationResDTO.ReservationCheckoutApprovalRes.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus().getDescription())
                .checkoutRejected(reservation.getCheckoutRejected())
                .checkoutSubmittedAt(reservation.getCheckoutSubmittedAt())
                .checkoutRejectedAt(reservation.getCheckoutRejectedAt())
                .build();
    }
}
