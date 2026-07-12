package com.popIt.pop_it.domain.identity_verification.repository;

import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {
}
