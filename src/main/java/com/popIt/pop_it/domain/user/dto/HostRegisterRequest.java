package com.popIt.pop_it.domain.user.dto;

import com.popIt.pop_it.domain.user.entity.enums.Bank;
import com.popIt.pop_it.domain.user.entity.enums.TaxationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HostRegisterRequest(

        @Schema(description = "과세자 종류", example = "SIMPLIFIED")
        @NotNull(message = "과세자 종류는 필수입니다.")
        TaxationType taxationType,

        // 하이픈 포함/미포함 모두 허용, 저장 시 숫자 10자리로 정규화
        @Schema(description = "사업자등록번호(하이픈 포함/미포함 모두 허용, 저장 시 숫자 10자리)", example = "123-45-67890")
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자등록번호는 숫자 10자리여야 합니다.")
        String businessRegistrationNumber,

        @Schema(description = "사업자등록증 사본 URL(S3)", example = "https://s3.example.com/business-license.png")
        @NotBlank(message = "사업자등록증 사본 URL은 필수입니다.")
        // S3 업로드 URL은 항상 https → http 허용 시 평문 전송 위험이 있어 https만 강제
        @Pattern(regexp = "^https://.+", message = "유효한 https URL이어야 합니다.")
        String businessLicenseUrl,

        @Schema(description = "상호명", example = "팝잇 상회")
        @NotBlank(message = "상호명은 필수입니다.")
        String businessName,

        @Schema(description = "사업장 주소(기본+상세 병합)", example = "서울시 강남구 테헤란로 1 3층 301호")
        @NotBlank(message = "사업장 주소는 필수입니다.")
        String businessAddress,

        @Schema(description = "은행", example = "KB")
        @NotNull(message = "은행은 필수입니다.")
        Bank bank,

        @Schema(description = "정산 계좌번호(숫자만)", example = "1234567890")
        @NotBlank(message = "정산 계좌번호는 필수입니다.")
        @Pattern(regexp = "^\\d+$", message = "정산 계좌번호는 숫자만 입력 가능합니다.")
        String settlementAccountNumber,

        @Schema(description = "예금주", example = "홍길동")
        @NotBlank(message = "예금주는 필수입니다.")
        @Size(max = 20, message = "예금주는 20자 이내여야 합니다.")
        String accountHolder,

        @Schema(description = "통장 사본 URL(S3)", example = "https://s3.example.com/bankbook.png")
        @NotBlank(message = "통장 사본 URL은 필수입니다.")
        // S3 업로드 URL은 항상 https → http 허용 시 평문 전송 위험이 있어 https만 강제
        @Pattern(regexp = "^https://.+", message = "유효한 https URL이어야 합니다.")
        String bankbookCopyUrl
) {

    // 저장용 정규화: 사업자등록번호에서 하이픈/공백 제거 후 숫자만 남긴다
    public String normalizedBusinessRegistrationNumber() {
        return businessRegistrationNumber.replaceAll("[^0-9]", "");
    }
}
