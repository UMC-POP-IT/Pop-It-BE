package com.popIt.pop_it.domain.user.repository;

import com.popIt.pop_it.domain.user.entity.HostProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HostProfileRepository extends JpaRepository<HostProfile, Long> {

    // 한 사용자당 호스트 프로필은 1개만 허용 → 등록 전 중복 여부 선검사
    boolean existsByUserId(Long userId);
}
