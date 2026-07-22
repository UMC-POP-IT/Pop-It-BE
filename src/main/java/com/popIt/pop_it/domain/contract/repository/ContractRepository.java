package com.popIt.pop_it.domain.contract.repository;

import com.popIt.pop_it.domain.contract.entity.Contract;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByReservation_Id(Long reservationId);

    // 동시 결제 준비 요청 간 중복 PENDING 생성 방지는 payment 테이블의
    // partial unique index(contract_id, status='PENDING')가 DB 레벨에서 담당
    @Query("select c from Contract c join fetch c.reservation r join fetch r.user where c.id = :contractId")
    Optional<Contract> findWithReservationAndUserById(Long contractId);
}
