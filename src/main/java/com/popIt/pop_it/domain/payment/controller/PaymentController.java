package com.popIt.pop_it.domain.payment.controller;

import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.exception.PaymentSuccessCode;
import com.popIt.pop_it.domain.payment.service.PaymentService;
import com.popIt.pop_it.domain.payment.service.PaymentWebhookService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "결제", description = "토스페이먼츠로 계약에 대한 결제를 진행합니다.")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService paymentWebhookService;

    @Operation(summary = "결제 요청(준비)", description = "계약을 완료하고 결제 요청를 요청합니다.")
    @PostMapping("/api/v1/contracts/{contractId}/payments")
    public ApiResponse<PaymentResDTO.Prepare> prepare(
            @Parameter(description = "결제를 준비할 계약 ID", example = "1")
            @PathVariable Long contractId,
            @Parameter(description = "중복 요청 방지를 위한 클라이언트 생성 키. 같은 키로 재요청 시 동일한 결제를 그대로 반환합니다.",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key는 필수입니다.")
            @Size(max = 100, message = "Idempotency-Key는 100자를 초과할 수 없습니다.")
            String idempotencyKey,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PaymentResDTO.Prepare resDTO = paymentService.prepare(
                contractId, idempotencyKey, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_PREPARED, resDTO);
    }

    @Operation(summary = "결제 승인", description = "토스페이먼츠 결제창에서 인증 완료 후 전달받은 정보로 결제 승인을 요청합니다. 가상결제는 지원하지 않습니다.")
    @PostMapping("/api/v1/payments/{paymentId}/confirm")
    public ApiResponse<PaymentResDTO.Confirm> confirm(
            @Parameter(description = "승인할 결제 ID", example = "1")
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentReqDTO.Confirm reqDTO,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PaymentResDTO.Confirm resDTO = paymentService.confirm(
                paymentId, reqDTO, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_CONFIRM, resDTO);
    }

    @Hidden
    @Operation(summary = "토스페이먼츠 웹훅", description = "결제 상태 변경 시 토스페이먼츠 서버가 호출하는 웹훅입니다. 가상결제는 지원하지 않습니다.")
    @PostMapping("/api/v1/payments/webhook")
    public ApiResponse<Void> webhook(@RequestBody PaymentReqDTO.Webhook payload) {
        paymentWebhookService.handle(payload);
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_WEBHOOK_RECEIVED, null);
    }
}
