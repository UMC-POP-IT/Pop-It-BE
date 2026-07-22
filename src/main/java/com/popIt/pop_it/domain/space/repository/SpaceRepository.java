package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.Space;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")) // 즉시실패
    @Query("select s from Space s where s.id = :spaceId")
    Optional<Space> findByIdForUpdate(@Param("spaceId") Long spaceId);

    // 소프트 삭제된 공간은 조회 대상에서 제외
    Optional<Space> findByIdAndDeletedAtIsNull(Long id);

    // 내 공간 목록 조회
    Page<Space> findAllByHostIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long hostId, Pageable pageable);
}
