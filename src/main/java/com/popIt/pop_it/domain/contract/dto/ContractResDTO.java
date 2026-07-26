package com.popIt.pop_it.domain.contract.dto;

import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import lombok.Builder;

import java.time.LocalDate;

public class ContractResDTO {
    @Builder
    public record ContractGuestPaymentInfoRes(
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
    public record ContractHostPaymentInfoRes(
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long platformFee,
            Long totalPrice
    ) implements ContractPaymentInfoRes {}

    @Builder
    public record ContractSignatureRes(
            ContractStatus contractStatus,
            Boolean bothSigned
    ){}

    public sealed interface ContractPaymentInfoRes permits ContractGuestPaymentInfoRes, ContractHostPaymentInfoRes {
    }

    @Builder
    public record ContractInfoRes(
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
