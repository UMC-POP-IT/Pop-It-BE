package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.Space;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")) // 3초
    @Query("select s from Space s where s.id = :spaceId")
    Optional<Space> findByIdForUpdate(@Param("spaceId") Long spaceId);
}
