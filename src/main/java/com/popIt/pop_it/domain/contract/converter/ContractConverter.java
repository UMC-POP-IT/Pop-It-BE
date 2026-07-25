package com.popIt.pop_it.domain.contract.converter;

import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.reservation.entity.Reservation;

public class ContractConverter {
    public static ContractResDTO.GetGuestContractPaymentInfoRes toGetGuestContractPaymentInfoRes(Contract contract, Reservation reservation) {
        return ContractResDTO.GetGuestContractPaymentInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .period(contract.getPeriod())
                .rentalFee(contract.getRentalFee())
                .deposit(contract.getDeposit())
                .insuranceFee(contract.getInsuranceFee())
                .totalPrice(contract.getTotalPrice())
                .build();
    }

    public static ContractResDTO.GetHostContractPaymentInfoRes toGetHostContractPaymentInfoRes(Contract contract, Reservation reservation) {
        return ContractResDTO.GetHostContractPaymentInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .period(contract.getPeriod())
                .rentalFee(contract.getRentalFee())
                .platformFee(contract.getPlatformFee())
                .totalPrice(reservation.getHostTotalPrice())
                .build();
    }

    public static ContractResDTO.ContractPaymentInfoRes toGetContractInfoRes(Contract contract, Reservation reservation) {
        return ContractResDTO.GetContractInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .roadAddress(reservation.getSpace().getRoadAddress())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .period(contract.getPeriod())
                .rentalFee(contract.getRentalFee())
                .deposit(contract.getDeposit())
                .insuranceFee(contract.getInsuranceFee())
                .build();
    }
}
