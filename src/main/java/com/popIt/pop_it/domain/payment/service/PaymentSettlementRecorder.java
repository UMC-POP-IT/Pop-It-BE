package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.exception.PaymentErrorCode;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PaymentSettlementRecorder {

    private final PaymentRepository paymentRepository;

    // 정산 각 단계의 결과를 별도 트랜잭션에 커밋한다.
    // settle()이 나중에 실패로 끝나 롤백되더라도, 이미 완료된 단계의 기록은 남아있어야
    // 다음 재시도에서 그 단계를 건너뛸 수 있다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(Long paymentId, Consumer<Payment> mutation) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ProjectException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        mutation.accept(payment);
    }
}
