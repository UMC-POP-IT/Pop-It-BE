package com.popIt.pop_it.domain.payment.service;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 결제 승인(confirm 응답 처리, 웹훅 반영) 양쪽에서 공통으로 쓰는 계약/예약 완료 처리.
// 이미 완료된 계약이면 멱등하게 스킵한다.
@Slf4j
@Component
@RequiredArgsConstructor
class ContractCompletionService {

    private final ContractRepository contractRepository;

    // alreadyPaid: 이 결제가 이번 호출 이전부터 이미 PAID였는지 - false면 방금 이 호출에서
    // 새로 PAID로 확정된 것이라, 계약이 이미 다른 결제로 완료돼 있으면 이중 승인(청구)으로 본다.
    public void completeIfNeeded(Payment payment, boolean alreadyPaid) {
        Contract contract = payment.getContract();
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            if (!alreadyPaid) {
                // 이 결제는 방금 PAID로 확정됐지만, 계약은 이미 다른 결제로 완료돼 있다 - 같은
                // 계약에 토스 승인이 두 번 들어온 이중 청구 상황. 결제 자체는 PAID로 남기고
                // (실제로 청구된 사실이므로) 환불 등 운영 처리가 필요함을 ERROR로 알린다.
                log.error("계약당 결제 중복 승인 감지(이중 청구 의심, 환불 필요): paymentId={}, contractId={}",
                        payment.getId(), contract.getId());
            }
            return;
        }
        // 계약 결제 완료 및 예약 결제 완료 처리
        contract.markAsCompleted();
        contract.getReservation().markPaymentCompleted();
        // Contract는 @Version이 걸려 있어, 다른 경로가 동시에 완료 처리하면 버전 충돌이 날 수
        // 있다. 호출부가 즉시 감지해 catch할 수 있도록 명시적으로 flush한다.
        contractRepository.saveAndFlush(contract);
    }
}
