package com.popIt.pop_it.domain.payment.controller;

import com.popIt.pop_it.domain.payment.dto.PaymentReqDTO;
import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.exception.code.PaymentSuccessCode;
import com.popIt.pop_it.domain.payment.service.PaymentService;
import com.popIt.pop_it.domain.payment.service.PaymentWebhookService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "결제", description = "토스페이먼츠 기반 계약 결제 API")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService paymentWebhookService;

    @Operation(summary = "결제 요청(준비)", description = "계약에 대한 결제를 준비합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "결제 준비 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Idempotency-Key 누락/형식 오류 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인의 계약이 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "계약을 찾을 수 없음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "결제 가능한 계약 상태가 아님, 동일 Idempotency-Key로 다른 요청 처리 중 충돌, 이미 결제 완료, 이전 결제 실패/만료로 재시도 필요, 계약 내용 변조 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/api/v1/contracts/{contractId}/payments")
    public ApiResponse<PaymentResDTO.PaymentPrepareRes> prepare(
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
        PaymentResDTO.PaymentPrepareRes resDTO = paymentService.prepare(
                contractId, idempotencyKey, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_PREPARED, resDTO);
    }

    @Operation(summary = "결제 승인", description = "토스페이먼츠 결제창에서 인증 완료 후 전달받은 정보로 결제 승인을 요청합니다. 가상결제는 지원하지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "결제 승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 입력값 누락/형식 오류, 주문번호 불일치, 결제 금액 불일치, 지원하지 않는 결제 수단, 토스 결제 승인 실패 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증되지 않음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 계약의 결제가 아님",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "결제를 찾을 수 없음",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이전 결제 실패/만료로 재시도 필요, 계약 처리 동시 진행으로 인한 충돌 포함",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502", description = "결제 게이트웨이(토스페이먼츠)와 통신할 수 없음",
                    content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/api/v1/payments/{paymentId}/confirm")
    public ApiResponse<PaymentResDTO.PaymentConfirmRes> confirm(
            @Parameter(description = "승인할 결제 ID", example = "1")
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentReqDTO.PaymentConfirmReq reqDTO,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PaymentResDTO.PaymentConfirmRes resDTO = paymentService.confirm(
                paymentId, reqDTO, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_CONFIRM, resDTO);
    }

    @Hidden
    @Operation(summary = "토스페이먼츠 웹훅", description = "결제 상태 변경 시 토스페이먼츠 서버가 호출하는 웹훅입니다. 가상결제는 지원하지 않습니다.")
    @PostMapping("/api/v1/payments/webhook")
    public ApiResponse<Void> webhook(@RequestBody PaymentReqDTO.PaymentWebhookReq payload) {
        paymentWebhookService.handle(payload);
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_WEBHOOK_RECEIVED, null);
    }
}
