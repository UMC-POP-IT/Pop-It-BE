package com.popIt.pop_it.domain.contract.dto;

import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import lombok.Builder;

import java.time.LocalDate;

public class ContractResDTO {
    @Builder
    public record GetGuestContractInfoRes (
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long deposit,
            Long insuranceFee,
            Long totalPrice
    ) implements ContractInfoRes {}

    @Builder
    public record GetHostContractInfoRes (
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long platformFee,
            Long totalPrice
    ) implements ContractInfoRes {}

    @Builder
    public record SignatureRes (
            ContractStatus contractStatus,
            Boolean bothSigned
    ){}

    public sealed interface ContractInfoRes permits GetGuestContractInfoRes, GetHostContractInfoRes {
    }


}
