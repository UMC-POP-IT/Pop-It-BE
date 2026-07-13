package com.popIt.pop_it.domain.payment.repository;

import com.popIt.pop_it.domain.payment.entity.Payment;
import java.util.Optional;

import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByContractIdAndStatus(Long contractId, PaymentStatus paymentStatus);
}
