package com.popIt.pop_it.domain.contract.repository;

import com.popIt.pop_it.domain.contract.entity.Contract;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("select c from Contract c join fetch c.reservation r join fetch r.user where c.id = :contractId")
    Optional<Contract> findWithReservationAndUserById(Long contractId);
}
