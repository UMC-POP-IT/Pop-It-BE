package com.popIt.pop_it.domain.contract.dto;

import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import lombok.Builder;

import java.time.LocalDate;

public class ContractResDTO {
    @Builder
    public record GetGuestContractPaymentInfoRes(
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long deposit,
            Long insuranceFee,
            Long totalPrice
    ) implements ContractPaymentInfoRes {}

    @Builder
    public record GetHostContractPaymentInfoRes(
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long platformFee,
            Long totalPrice
    ) implements ContractPaymentInfoRes {}

    @Builder
    public record SignatureRes (
            ContractStatus contractStatus,
            Boolean bothSigned
    ){}

    public sealed interface ContractPaymentInfoRes permits GetGuestContractPaymentInfoRes, GetHostContractPaymentInfoRes {
    }

    @Builder
    public record GetContractInfoRes(
            String spaceName,
            String roadAddress,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long deposit,
            Long insuranceFee
    ) {}
}
