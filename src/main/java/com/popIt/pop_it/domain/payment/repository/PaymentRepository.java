package com.popIt.pop_it.domain.payment.repository;

import com.popIt.pop_it.domain.payment.entity.Payment;
import java.util.List;
import java.util.Optional;

import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.enums.SettlementStepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByContractIdAndStatus(Long contractId, PaymentStatus paymentStatus);

    Optional<Payment> findByContractReservationIdAndStatus(Long reservationId, PaymentStatus status);

    // PENDING/FAILED일 때만 PROCESSING으로 조건부 전환한다(DB 레벨 선점).
    // 반환값이 0이면 이미 다른 실행이 선점했거나(PROCESSING) 완료(DONE)된 것이므로
    // 호출부는 외부 API를 다시 호출하면 안 된다.
    @Modifying
    @Query("update Payment p set p.hostPayoutStatus = :target "
            + "where p.id = :paymentId and p.hostPayoutStatus in :allowed")
    int claimHostPayoutForProcessing(
            @Param("paymentId") Long paymentId,
            @Param("target") SettlementStepStatus target,
            @Param("allowed") List<SettlementStepStatus> allowed);

    default int claimHostPayoutForProcessing(Long paymentId) {
        return claimHostPayoutForProcessing(
                paymentId,
                SettlementStepStatus.PROCESSING,
                List.of(SettlementStepStatus.PENDING, SettlementStepStatus.FAILED));
    }

    @Modifying
    @Query("update Payment p set p.depositRefundStatus = :target "
            + "where p.id = :paymentId and p.depositRefundStatus in :allowed")
    int claimDepositRefundForProcessing(
            @Param("paymentId") Long paymentId,
            @Param("target") SettlementStepStatus target,
            @Param("allowed") List<SettlementStepStatus> allowed);

    default int claimDepositRefundForProcessing(Long paymentId) {
        return claimDepositRefundForProcessing(
                paymentId,
                SettlementStepStatus.PROCESSING,
                List.of(SettlementStepStatus.PENDING, SettlementStepStatus.FAILED));
    }
}
