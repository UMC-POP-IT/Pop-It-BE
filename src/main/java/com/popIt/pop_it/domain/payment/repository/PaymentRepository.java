package com.popIt.pop_it.domain.payment.repository;

import com.popIt.pop_it.domain.payment.entity.Payment;
import java.util.List;
import java.util.Optional;

import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.payment.enums.SettlementStepStatus;
import org.springframework.data.domain.Pageable;
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

    // 보증금 환불을 자동 대상에서 제외할 때, PENDING일 때만 SKIPPED로 조건부 전환한다.
    // 그 사이 다른 경로가 이미 PROCESSING/DONE/FAILED로 바꿔놨다면 0건에 그쳐 그 상태를 덮어쓰지 않는다.
    @Modifying
    @Query("update Payment p set p.depositRefundStatus = :target "
            + "where p.id = :paymentId and p.depositRefundStatus = :expected")
    int compareAndSetDepositRefundStatus(
            @Param("paymentId") Long paymentId,
            @Param("target") SettlementStepStatus target,
            @Param("expected") SettlementStepStatus expected);

    default int skipDepositRefundIfPending(Long paymentId) {
        return compareAndSetDepositRefundStatus(
                paymentId, SettlementStepStatus.SKIPPED, SettlementStepStatus.PENDING);
    }

    // 동시 confirm() 호출 중 하나가 뒤늦게 실패를 기록하려 할 때, 다른 하나가 이미 PAID로
    // 커밋한 상태를 덮어쓰지 않도록 조건부로 전환한다(status가 여전히 PENDING일 때만 FAILED/EXPIRED로).
    @Modifying
    @Query("update Payment p set p.status = :target where p.id = :paymentId and p.status = :expected")
    int compareAndSetStatus(
            @Param("paymentId") Long paymentId,
            @Param("target") PaymentStatus target,
            @Param("expected") PaymentStatus expected);

    default int markFailedIfPending(Long paymentId) {
        return compareAndSetStatus(paymentId, PaymentStatus.FAILED, PaymentStatus.PENDING);
    }

    default int markExpiredIfPending(Long paymentId) {
        return compareAndSetStatus(paymentId, PaymentStatus.EXPIRED, PaymentStatus.PENDING);
    }

    // 정산 단계(호스트 지급/보증금 환불) 중 하나라도 FAILED로 남아있는 결제 건을 ID 오름차순으로 배치 조회한다.
    // afterId 기준 keyset 방식이라, 배치 처리 중 앞선 건의 상태가 바뀌어도(재시도 성공 등) 뒤 배치의
    // 조회 결과가 밀리거나 겹치지 않는다(오프셋 페이징과 달리 skip/중복 없음).
    @Query("select p from Payment p where p.id > :afterId and p.status = :status "
            + "and (p.hostPayoutStatus = :failed or p.depositRefundStatus = :failed) "
            + "order by p.id asc")
    List<Payment> findBatchOfSettlementStepFailed(
            @Param("afterId") Long afterId,
            @Param("status") PaymentStatus status,
            @Param("failed") SettlementStepStatus failed,
            Pageable pageable);

    default List<Payment> findNextFailedSettlementBatch(Long afterId, Pageable pageable) {
        return findBatchOfSettlementStepFailed(
                afterId, PaymentStatus.PAID, SettlementStepStatus.FAILED, pageable);
    }
}
