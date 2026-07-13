package com.popIt.pop_it.domain.identity_verification.repository;

import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    IdentityVerification findByUser(User user);

    boolean existsByIdentityVerificationId(@NotBlank String s);

    boolean existsByCiHash(String ciHash);
}
