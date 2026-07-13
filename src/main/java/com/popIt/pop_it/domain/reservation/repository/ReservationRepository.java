package com.popIt.pop_it.domain.reservation.repository;

import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Contract> findBySpaceId(Long reservationId);
}
