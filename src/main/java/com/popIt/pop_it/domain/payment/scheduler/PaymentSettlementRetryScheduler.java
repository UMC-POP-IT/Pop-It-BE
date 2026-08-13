package com.popIt.pop_it.domain.payment.scheduler;

import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.domain.payment.service.PaymentService;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 정산(호스트 지급/보증금 환불) 단계 중 하나라도 FAILED로 남은 결제 건을 주기적으로 재시도한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSettlementRetryScheduler {

    private static final int BATCH_SIZE = 200;

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Scheduled(cron = "0 0 8,15 * * *", zone = "Asia/Seoul") // 매일 오전 8시, 오후 3시
    public void retryFailedSettlements() {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        long cursor = 0L;
        List<Payment> batch;

        do {
            batch = paymentRepository.findNextFailedSettlementBatch(cursor, pageable);

            for (Payment payment : batch) {
                try {
                    paymentService.settle(payment.getId());
                    log.info("정산 실패 단계 재시도 성공 - paymentId: {}", payment.getId());
                } catch (ProjectException e) {
                    log.warn("정산 실패 단계 재시도 실패 - paymentId: {}, code: {}", payment.getId(), e.getErrorCode(), e);
                } catch (Exception e) {
                    log.error("정산 실패 단계 재시도 중 예상치 못한 오류 - paymentId: {}", payment.getId(), e);
                }
            }

            if (!batch.isEmpty()) {
                cursor = batch.get(batch.size() - 1).getId();
            }
        } while (batch.size() == BATCH_SIZE);
    }
}
