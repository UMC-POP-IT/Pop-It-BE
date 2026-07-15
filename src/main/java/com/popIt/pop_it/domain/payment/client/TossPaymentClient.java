package com.popIt.pop_it.domain.payment.client;

import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.exception.TossErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class TossPaymentClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final RestClient restClient;

    public TossPaymentClient(@Value("${toss.secret-key}") String secretKey) {
        // 토스페이먼츠 API는 시크릿 키를 사용자 ID로, 비밀번호 없이 Basic 인증에 사용
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .baseUrl(CONFIRM_URL)
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();
    }

    public PaymentResDTO.TossConfirm confirm(String paymentKey, String orderId, Long amount) {
        try {
            return restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(PaymentResDTO.TossConfirm.class);
        } catch (RestClientResponseException e) {
            PaymentResDTO.TossError tossError = parseTossError(e);
            log.warn("토스 결제 승인 실패: status={}, code={}, message={}",
                    e.getStatusCode(), tossError.code(), tossError.message());
            throw new ProjectException(new TossErrorCode(
                    HttpStatus.valueOf(e.getStatusCode().value()), tossError.code(), tossError.message()));
        }
    }

    private PaymentResDTO.TossError parseTossError(RestClientResponseException e) {
        try {
            return e.getResponseBodyAs(PaymentResDTO.TossError.class);
        } catch (Exception parseException) {
            return new PaymentResDTO.TossError("UNKNOWN_PAYMENT_ERROR", "결제 승인에 실패했습니다.");
        }
    }

    private record ConfirmRequest(String paymentKey, String orderId, Long amount) {
    }
}
