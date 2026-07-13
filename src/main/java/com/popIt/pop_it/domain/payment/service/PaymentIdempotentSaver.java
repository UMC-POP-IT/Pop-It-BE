package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PaymentIdempotentSaver {

    private final PaymentRepository paymentRepository;

    // 별도 트랜잭션에서 저장을 시도한다. UNIQUE 제약 위반 시 이 트랜잭션 안에서만
    // 예외를 처리하고 롤백하므로, 호출한 쪽의 영속성 컨텍스트는 오염되지 않는다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment save(Payment payment, String idempotencyKey) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
    }
}
