package com.popIt.pop_it.domain.user.dto;

import com.popIt.pop_it.domain.user.entity.enums.Bank;
import com.popIt.pop_it.domain.user.entity.enums.TaxationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record HostProfileRes(
        @Schema(description = "호스트 프로필 ID", example = "1")
        Long id,

        @Schema(description = "과세자 종류", example = "SIMPLIFIED")
        TaxationType taxationType,

        @Schema(description = "사업자등록번호(숫자 10자리)", example = "1234567890")
        String businessRegistrationNumber,

        @Schema(description = "사업자등록증 사본 URL(S3)", example = "https://pop-it-host-documents.s3.ap-northeast-2.amazonaws.com/host-document/10/uuid.png")
        String businessLicenseUrl,

        @Schema(description = "상호명", example = "팝잇 상회")
        String businessName,

        @Schema(description = "사업장 주소(기본+상세 병합)", example = "서울시 강남구 테헤란로 1 3층 301호")
        String businessAddress,

        @Schema(description = "통장 사본 URL(S3)", example = "https://pop-it-host-documents.s3.ap-northeast-2.amazonaws.com/host-document/10/uuid.png")
        String bankbookCopyUrl,

        @Schema(description = "은행", example = "KB")
        Bank bank,

        @Schema(description = "정산 계좌번호(숫자만)", example = "1234567890")
        String settlementAccountNumber,

        @Schema(description = "예금주", example = "홍길동")
        String accountHolder,

        @Schema(description = "호스트 등록 일시", example = "2026-08-04T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "유저 ID", example = "1")
        Long userId
) {}
