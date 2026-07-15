package com.popIt.pop_it.domain.contract.dto;

import java.time.LocalDate;

public class ContractResDTO {
    public record GetGuestContractInfoRes (
            Long contractId,
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long deposit,
            Long insuranceFee
    ) {}

    public record GetHostContractInfoRes (
            Long contractId,
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long platformFee
    ) {}

    public record SignatureRes (
            String reservationStatus,
            Boolean bothSigned
    ){}
}
