package com.popIt.pop_it.domain.contract.dto;

import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import lombok.Builder;

import java.time.LocalDate;

public class ContractResDTO {
    @Builder
    public record ContractGuestInfoRes(
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
    public record ContractHostInfoRes(
            String spaceName,
            LocalDate startDate,
            LocalDate endDate,
            Long period,
            Long rentalFee,
            Long platformFee,
            Long totalPrice
    ) implements ContractInfoRes {}

    @Builder
    public record ContractSignatureRes(
            ContractStatus contractStatus,
            Boolean bothSigned
    ){}

    public sealed interface ContractInfoRes permits ContractGuestInfoRes, ContractHostInfoRes {
    }


}
