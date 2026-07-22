package com.popIt.pop_it.domain.identity_verification.repository;

import com.popIt.pop_it.domain.identity_verification.entity.IdentityVerification;
import com.popIt.pop_it.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    IdentityVerification findByUser(User user);

    boolean existsByIdentityVerificationId(@NotBlank String s);

    boolean existsByCiHash(String ciHash);

    // ciHash 칼럼만 조회합니다.
    // IdentityVerification을 통채로 조회하면 암호화 필드 전부 복호화되므로 별도의 jpql 을 통해 조회하도록 합니다.
    @Query("select i.ciHash from IdentityVerification  i where i.user = :user")
    Optional<String> findCiHashByUser(@Param("user") User user);
}
