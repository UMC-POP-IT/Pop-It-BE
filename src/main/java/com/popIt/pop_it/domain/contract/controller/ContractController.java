package com.popIt.pop_it.domain.contract.controller;

import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.exception.code.ContractSuccessCode;
import com.popIt.pop_it.domain.contract.service.ContractService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reservations/{reservationId}/contract")
public class ContractController {

    private final ContractService contractService;

    // 결제/임대 예정 계약 정보 조회
    // @TODO: 기본값은 Guest 기반 반환. 계약 기능 구현 시 세분화할 예정
    @GetMapping
    public ApiResponse<ContractResDTO.GetGuestContractInfoRes> getContractInfo(
            @PathVariable Long reservationId
    ) {
        BaseSuccessCode code = ContractSuccessCode.CONTRACT_INFO_FOUND;
        return ApiResponse.onSuccess(code, contractService.getContractInfo(reservationId));
    }

    // 전자서명 제출
    @PostMapping("/signatures")
    public ApiResponse<ContractResDTO.SignatureRes> signature(
            @PathVariable Long reservationId,
            @RequestBody ContractReqDTO.SignatureReq dto
            ) {
        BaseSuccessCode code = ContractSuccessCode.SIGNATURE_SUCCESS;
        return ApiResponse.onSuccess(code, contractService.signature(reservationId, dto));
    }

}
