package com.popIt.pop_it.domain.contract.converter;

import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.reservation.entity.Reservation;

public class ContractConverter {
    public static ContractResDTO.ContractGuestInfoRes toGetGuestContractInfoRes(Reservation reservation) {
        return ContractResDTO.ContractGuestInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .period(reservation.getPeriod())
                .rentalFee(reservation.getRentalFee())
                .deposit(reservation.getDeposit())
                .insuranceFee(reservation.getInsuranceFee())
                .totalPrice(reservation.getTotalPrice())
                .build();
    }

    public static ContractResDTO.ContractHostInfoRes toGetHostContractInfoRes(Reservation reservation) {
        return ContractResDTO.ContractHostInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .period(reservation.getPeriod())
                .rentalFee(reservation.getRentalFee())
                .platformFee(reservation.getPlatformFee())
                .totalPrice(reservation.getHostTotalPrice())
                .build();
    }
}
