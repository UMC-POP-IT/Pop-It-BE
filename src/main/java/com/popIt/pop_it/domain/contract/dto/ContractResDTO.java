package com.popIt.pop_it.domain.contract.dto;

import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

public class ContractResDTO {
    @Builder
    public record ContractGuestPaymentInfoRes(
            @Schema(description = "공간 건물명", example = "팝잇 빌딩")
            String spaceName,

            @Schema(description = "이용 시작일", example = "2026-08-01")
            LocalDate startDate,

            @Schema(description = "이용 종료일", example = "2026-08-03")
            LocalDate endDate,

            @Schema(description = "이용 기간(일, 시작/종료일 포함)", example = "3")
            Long period,

            @Schema(description = "대여료", example = "200000")
            Long rentalFee,

            @Schema(description = "보증금", example = "1000000")
            Long deposit,

            @Schema(description = "보험료", example = "10000")
            Long insuranceFee,

            @Schema(description = "총 결제 금액(임대료+보증금+보험료)", example = "1210000")
            Long totalPrice
    ) implements ContractPaymentInfoRes {}

    @Builder
    public record ContractHostPaymentInfoRes(
            @Schema(description = "공간 건물명", example = "팝잇 빌딩")
            String spaceName,

            @Schema(description = "이용 시작일", example = "2026-08-01")
            LocalDate startDate,

            @Schema(description = "이용 종료일", example = "2026-08-03")
            LocalDate endDate,

            @Schema(description = "이용 기간(일, 시작/종료일 포함)", example = "3")
            Long period,

            @Schema(description = "대여료", example = "200000")
            Long rentalFee,

            @Schema(description = "플랫폼 수수료", example = "20000")
            Long platformFee,

            @Schema(description = "호스트 입금 예정 금액(임대료-플랫폼 수수료)", example = "180000")
            Long totalPrice
    ) implements ContractPaymentInfoRes {}

    @Builder
    public record ContractSignatureRes(
            @Schema(description = "계약 ID", example = "1")
            Long contractId,

            @Schema(
                    description = "계약 상태. HOST_SIGNATURE_PENDING(호스트 서명대기) → GUEST_SIGNATURE_PENDING(게스트 서명대기) → PENDING_PAYMENT(결제 대기) → COMPLETED(결제 완료) 순으로 전이",
                    example = "GUEST_SIGNATURE_PENDING"
            )
            ContractStatus contractStatus,

            @Schema(description = "호스트/게스트 양측 서명이 모두 완료됐는지 여부", example = "false")
            Boolean bothSigned
    ){}

    public sealed interface ContractPaymentInfoRes permits ContractGuestPaymentInfoRes, ContractHostPaymentInfoRes {
    }

    @Builder
    public record ContractInfoRes(
            @Schema(description = "계약 ID", example = "1")
            Long contractId,

            @Schema(description = "공간 건물명", example = "팝잇 빌딩")
            String spaceName,

            @Schema(description = "공간 도로명 주소", example = "서울특별시 마포구 홍익로 123")
            String roadAddress,

            @Schema(description = "이용 시작일", example = "2026-08-01")
            LocalDate startDate,

            @Schema(description = "이용 종료일", example = "2026-08-03")
            LocalDate endDate,

            @Schema(description = "이용 기간(일, 시작/종료일 포함)", example = "3")
            Long period,

            @Schema(description = "대여료", example = "200000")
            Long rentalFee,

            @Schema(description = "보증금", example = "1000000")
            Long deposit,

            @Schema(description = "보험료", example = "10000")
            Long insuranceFee
    ) {}
}
