package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.exception.code.PaymentErrorCode;
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

    // 외부 API를 호출하기 전에 DB에서 조건부 UPDATE로 단일 실행권을 선점한다.
    // 두 실행이 동시에 들어와도 이 UPDATE는 행 잠금을 거쳐 순차적으로 처리되므로,
    // 먼저 커밋된 쪽만 PENDING/FAILED -> PROCESSING 전환에 성공하고 나머지는 0건으로 실패한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimHostPayout(Long paymentId) {
        return paymentRepository.claimHostPayoutForProcessing(paymentId) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimDepositRefund(Long paymentId) {
        return paymentRepository.claimDepositRefundForProcessing(paymentId) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void skipDepositRefundIfPending(Long paymentId) {
        paymentRepository.skipDepositRefundIfPending(paymentId);
    }

    // confirm()이 실패를 기록할 때 사용한다. 동시에 다른 confirm() 호출이 먼저 PAID로 커밋했다면
    // status가 더 이상 PENDING이 아니므로 이 UPDATE는 0건에 그쳐 PAID를 덮어쓰지 않는다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markConfirmFailedIfPending(Long paymentId) {
        return paymentRepository.markFailedIfPending(paymentId) > 0;
    }
}
