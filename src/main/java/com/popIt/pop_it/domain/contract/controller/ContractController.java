package com.popIt.pop_it.domain.contract.controller;

import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.exception.code.ContractSuccessCode;
import com.popIt.pop_it.domain.contract.service.ContractService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "계약")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reservations/{reservationId}/contract")
public class ContractController {

    private final ContractService contractService;

    // 결제/임대 예정 계약 정보 조회
    @Operation(summary = "결제/임대 예정 계약 정보 조회", description = "계약서를 보기 전, “공간 + 기간 + 결제 예정(게스트)/입금 예정(호스트) 금액”을 조회합니다. 호스트/게스트가 받는 응답이 다릅니다. ")
    @GetMapping("/payment-preview")
    public ApiResponse<ContractResDTO.ContractPaymentInfoRes> getContractPaymentInfo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId
    ) {
        BaseSuccessCode code = ContractSuccessCode.CONTRACT_PAYMENT_INFO_FOUND;
        return ApiResponse.onSuccess(code, contractService.getContractPaymentInfo(authUser.getUser(), reservationId));
    }

    // 계약서 조회
    @Operation(summary = "계약서 조회", description = "계약을 하기 위해 필요한 계약서 정보를 조회합니다. 게스트와 호스트는 같은 응답(동일 계약서)을 받습니다. ")
    @GetMapping("/contract-preview")
    public ApiResponse<ContractResDTO.ContractInfoRes> getContractInfo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId
    ) {
        BaseSuccessCode code = ContractSuccessCode.CONTRACT_INFO_FOUND;
        return ApiResponse.onSuccess(code, contractService.getContractInfo(authUser.getUser(), reservationId));
    }


    // 전자서명 제출
    @Operation(summary = "전자서명 제출", description = "호스트/게스트가 계약서에 전자 서명합니다. 양측 서명 완료 시 계약이 체결됩니다. ")
    @PostMapping("/signatures")
    public ApiResponse<ContractResDTO.ContractSignatureRes> signature(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId,
            @RequestBody @Valid ContractReqDTO.ContractSignatureReq dto
            ) {
        BaseSuccessCode code = ContractSuccessCode.SIGNATURE_SUCCESS;
        return ApiResponse.onSuccess(code, contractService.signature(authUser.getUser(), reservationId, dto));
    }

}
