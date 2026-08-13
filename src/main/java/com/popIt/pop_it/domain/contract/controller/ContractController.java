package com.popIt.pop_it.domain.contract.controller;

import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.exception.code.ContractSuccessCode;
import com.popIt.pop_it.domain.contract.service.ContractService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "계약", description = "계약 정보/계약서 조회 및 전자서명 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations/{reservationId}/contracts")
public class ContractController {

    private final ContractService contractService;

    // 결제/임대 예정 계약 정보 조회
    @Operation(summary = "결제/임대 예정 계약 정보 조회", description = "계약서를 보기 전, [게스트(공간 + 기간 + 결제 예정)]/[호스트(입금 예정)] 금액을 조회합니다. 호스트/게스트가 받는 응답이 다릅니다. "
            + "contractId, contractStatus를 함께 반환하므로, 결제 화면을 벗어났다가 재진입할 때도 이 API로 계약 상태를 확인하고 결제(POST /contracts/{contractId}/payments)를 재개할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "결제/입금 예정 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인의 예약이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 예약, 존재하지 않는 계약 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "계약서 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인의 예약이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 예약, 존재하지 않는 계약 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @GetMapping("/contract-preview")
    public ApiResponse<ContractResDTO.ContractInfoRes> getContractInfo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId
    ) {
        BaseSuccessCode code = ContractSuccessCode.CONTRACT_INFO_FOUND;
        return ApiResponse.onSuccess(code, contractService.getContractInfo(authUser.getUser(), reservationId));
    }


    // 전자서명 제출
    @Operation(
            summary = "전자서명 제출",
            description = """
                    호스트/게스트가 계약서에 전자 서명합니다.
                    - 양측 서명 완료 시 계약이 체결됩니다.
                    - 본인인증이 선행되어야 합니다.
                    - 호스트가 먼저 서명해야 게스트가 서명할 수 있습니다.
                    - 계약 상태 전이: HOST_SIGNATURE_PENDING(호스트 서명대기) → GUEST_SIGNATURE_PENDING(게스트 서명대기) → PENDING_PAYMENT(결제 대기) → COMPLETED(결제 완료)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "전자서명 제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 입력값 누락/형식 오류(서명 이미지 URL), 허용되지 않은 버킷/경로의 URL 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인의 예약이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 예약, 존재하지 않는 계약 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "본인인증 미완료, 계약 내용 변조, 이미 서명 완료(호스트/전체), 서명 순서 위반, 동시 수정 충돌 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "서명 이미지 위변조 검증(S3 해시 계산) 중 오류",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/signatures")
    public ResponseEntity<ApiResponse<ContractResDTO.ContractSignatureRes>> signature(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId,
            @RequestBody @Valid ContractReqDTO.ContractSignatureReq dto
            ) {
        BaseSuccessCode code = ContractSuccessCode.SIGNATURE_SUCCESS;
        ContractResDTO.ContractSignatureRes result = contractService.signature(authUser.getUser(), reservationId, dto);
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.onSuccess(code, result));
    }

}
