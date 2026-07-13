package com.popIt.pop_it.domain.payment.controller;

import com.popIt.pop_it.domain.payment.dto.PaymentPrepareResponseDTO;
import com.popIt.pop_it.domain.payment.enums.PaymentSuccessCode;
import com.popIt.pop_it.domain.payment.service.PaymentService;
import com.popIt.pop_it.global.apiPayload.ApiResponse;
import com.popIt.pop_it.global.security.entity.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/v1/contracts/{contractId}/payments")
    public ApiResponse<PaymentPrepareResponseDTO> prepare(
            @PathVariable Long contractId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        PaymentPrepareResponseDTO result = paymentService.prepare(
                contractId, idempotencyKey, authUser.getUser().getUserId());
        return ApiResponse.onSuccess(PaymentSuccessCode.PAYMENT_PREPARED, result);
    }
}
