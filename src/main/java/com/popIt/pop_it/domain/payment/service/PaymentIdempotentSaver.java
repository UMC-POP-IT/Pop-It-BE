package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.payment.entity.Payment;
import com.popIt.pop_it.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PaymentIdempotentSaver {

    private final PaymentRepository paymentRepository;

    // 별도 트랜잭션에서 저장을 시도한다. UNIQUE 제약 위반 시 이 트랜잭션은 그대로 롤백되며
    // 예외를 호출한 쪽으로 전파한다. 실패한 이 트랜잭션의 영속성 컨텍스트로 곧바로 재조회하면
    // 오염된 세션을 계속 쓰게 되므로, 재조회는 호출한 쪽의 정상 트랜잭션에서 수행해야 한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment save(Payment payment) {
        return paymentRepository.saveAndFlush(payment);
    }
}
