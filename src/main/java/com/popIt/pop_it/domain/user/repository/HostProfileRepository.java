package com.popIt.pop_it.domain.user.repository;

import com.popIt.pop_it.domain.user.entity.HostProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HostProfileRepository extends JpaRepository<HostProfile, Long> {

    // 한 사용자당 호스트 프로필은 1개만 허용 → 등록 전 중복 여부 선검사
    boolean existsByUserId(Long userId);

    // 동일 사업자등록번호로 다른 사용자가 중복 등록하는지 해시로 선검사
    boolean existsByBusinessRegistrationNumberHash(String businessRegistrationNumberHash);

    Optional<HostProfile> findByUserId(Long userId);
}
