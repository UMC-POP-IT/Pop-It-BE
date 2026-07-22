package com.popIt.pop_it.domain.contract.repository;

import com.popIt.pop_it.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByReservation_Id(Long reservationId);
}
