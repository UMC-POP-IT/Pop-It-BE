package com.popIt.pop_it.domain.payment.controller;

import com.popIt.pop_it.domain.payment.dto.PaymentPrepareResponseDTO;
import com.popIt.pop_it.domain.payment.enums.PaymentSuccessCode;
import com.popIt.pop_it.domain.payment.service.PaymentService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "결제", description = "토스페이먼츠로 계약에 대한 결제를 진행합니다.")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 요청(준비)", description = "계약을 완료하고 결제 요청를 요청합니다.")
    @PostMapping("/api/v1/contracts/{contractId}/payments")
    public ApiResponse<PaymentPrepareResponseDTO> prepare(
            @Parameter(description = "결제를 준비할 계약 ID", example = "1")
            @PathVariable Long contractId,
            @Parameter(description = "중복 요청 방지를 위한 클라이언트 생성 키. 같은 키로 재요청 시 동일한 결제를 그대로 반환합니다.",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PaymentPrepareResponseDTO result = paymentService.prepare(
                contractId, idempotencyKey, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_PREPARED, result);
    }
}
