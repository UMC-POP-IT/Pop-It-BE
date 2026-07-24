package com.popIt.pop_it.domain.user_agreement.repository;

import com.popIt.pop_it.domain.user_agreement.entity.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {

    // 저장 요청에 포함된 약관들의 기존 동의 이력을 한 번에 조회 (upsert 판별용)
    List<UserAgreement> findAllByUserIdAndTermIdIn(Long userId, List<Long> termIds);
}
