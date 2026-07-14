package com.popIt.pop_it.domain.reservation.converter;

import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.user.entity.User;

public class ReservationConverter {
    public static ReservationResDTO.Summary toSummary(
            Reservation reservation, boolean includeGuest, String thumbnailUrl
    ) {
        return ReservationResDTO.Summary.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus().getDescription())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .usagePurpose(reservation.getUsagePurpose())
                .totalPrice(reservation.getTotalPrice())
                .isPhotoVerified(reservation.getCheckoutSubmittedAt() != null)
                .space(toSpaceSummary(reservation.getSpace(), thumbnailUrl))
                .guest(includeGuest ? toGuestSummary(reservation.getUser()) : null)
                .build();
    }

    private static ReservationResDTO.SpaceSummary toSpaceSummary(Space space, String thumbnailUrl) {
        return ReservationResDTO.SpaceSummary.builder()
                .spaceId(space.getId())
                .buildingName(space.getBuildingName())
                .address(space.getRoadAddress())
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private static ReservationResDTO.GuestSummary toGuestSummary(User user) {
        return ReservationResDTO.GuestSummary.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .build();
    }

    public static ReservationResDTO.CreateRes toCreateResult(Reservation reservation) {
        return ReservationResDTO.CreateRes.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus().getDescription())
                .rentalFee(reservation.getRentalFee())
                .deposit(reservation.getDeposit())
                .insuranceFee(reservation.getInsuranceFee())
                .totalPrice(reservation.getTotalPrice())
                .build();
    }

    public static ReservationResDTO.StatusChange toStatusChange(Reservation reservation) {
        return ReservationResDTO.StatusChange.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus().getDescription())
                .build();
    }
}
