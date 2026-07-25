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

    // 소프트 삭제 확인과 락 획득을 하나의 원자적 쿼리로 처리 (조회~락 사이 소프트 삭제 레이스 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")) // 즉시실패
    @Query("select s from Space s where s.id = :spaceId and s.deletedAt is null")
    Optional<Space> findByIdAndDeletedAtIsNullForUpdate(@Param("spaceId") Long spaceId);

    // 소프트 삭제된 공간은 조회 대상에서 제외
    Optional<Space> findByIdAndDeletedAtIsNull(Long id);

    // 소프트 삭제된 공간은 존재 여부 확인 대상에서 제외
    boolean existsByIdAndDeletedAtIsNull(Long id);

    // 내 공간 목록 조회
    Page<Space> findAllByHostIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long hostId, Pageable pageable);
}
