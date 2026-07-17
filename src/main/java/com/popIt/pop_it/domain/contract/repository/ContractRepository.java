package com.popIt.pop_it.domain.contract.repository;

import com.popIt.pop_it.domain.contract.entity.Contract;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    // 비관적 락: 같은 계약에 대한 동시 결제 준비 요청을 직렬화해
    // 서로 다른 Idempotency-Key로 들어와도 PENDING 결제가 중복 생성되지 않게 한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contract c join fetch c.reservation r join fetch r.user where c.id = :contractId")
    Optional<Contract> findWithReservationAndUserById(Long contractId);
}
