package com.popIt.pop_it.domain.payment.client;

import com.popIt.pop_it.domain.payment.dto.PaymentResDTO;
import com.popIt.pop_it.domain.payment.exception.PaymentErrorCode;
import com.popIt.pop_it.domain.payment.exception.TossErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class TossPaymentClient {

    private static final String BASE_URL = "https://api.tosspayments.com/v1/payments";

    private final RestClient restClient;

    public TossPaymentClient(@Value("${toss.secret-key}") String secretKey) {
        // 토스페이먼츠 API는 시크릿 키를 사용자 ID로, 비밀번호 없이 Basic 인증에 사용
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();
    }

    public PaymentResDTO.TossConfirm confirm(String paymentKey, String orderId, Long amount) {
        return post("/confirm", new ConfirmRequest(paymentKey, orderId, amount), PaymentResDTO.TossConfirm.class);
    }

    /**
     * 결제 금액 일부 취소
     *
     * @see <a href="https://docs.tosspayments.com/reference/error-codes#결제-취소">결제 취소 에러코드 전체 목록</a>
     */
    public PaymentResDTO.TossCancel cancelPartial(String paymentKey, Long cancelAmount, String cancelReason) {
        return post("/{paymentKey}/cancel", new CancelRequest(cancelAmount, cancelReason),
                PaymentResDTO.TossCancel.class, paymentKey);
    }

    /**
     * 결제 단건 조회. 웹훅 payload는 서명 검증이 없으므로, 웹훅이 알려준 paymentKey로
     * 이 API를 호출해 실제 결제 상태를 재확인한 뒤에만 그 결과를 신뢰해야 한다.
     */
    public PaymentResDTO.TossConfirm getPayment(String paymentKey) {
        return execute(() -> restClient.get()
                .uri("/{paymentKey}", paymentKey)
                .retrieve()
                .body(PaymentResDTO.TossConfirm.class));
    }

    private <T> T post(String uriTemplate, Object requestBody, Class<T> responseType, Object... uriVariables) {
        return execute(() -> restClient.post()
                .uri(uriTemplate, uriVariables)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(responseType));
    }

    private <T> T execute(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            PaymentResDTO.TossError tossError = parseTossError(e);
            log.warn("토스 결제 API 실패: status={}, code={}, message={}",
                    e.getStatusCode(), tossError.code(), tossError.message());
            throw new ProjectException(new TossErrorCode(
                    HttpStatus.valueOf(e.getStatusCode().value()), tossError.code(), tossError.message()));
        } catch (RestClientException e) {
            // 타임아웃, 연결 실패 등 HTTP 응답 자체를 받지 못한 경우
            log.warn("토스 결제 API 통신 실패", e);
            throw new ProjectException(PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE, e);
        }
    }

    private PaymentResDTO.TossError parseTossError(RestClientResponseException e) {
        try {
            return e.getResponseBodyAs(PaymentResDTO.TossError.class);
        } catch (Exception parseException) {
            return new PaymentResDTO.TossError("UNKNOWN_PAYMENT_ERROR", "결제 처리에 실패했습니다.");
        }
    }

    private record ConfirmRequest(String paymentKey, String orderId, Long amount) {
    }

    private record CancelRequest(Long cancelAmount, String cancelReason) {
    }
}
