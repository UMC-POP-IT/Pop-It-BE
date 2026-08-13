package com.popIt.pop_it.domain.identity_verification.entity;

import com.popIt.pop_it.domain.identity_verification.entity.enums.Gender;
import com.popIt.pop_it.domain.identity_verification.entity.enums.PortOneVerificationStatus;
import com.popIt.pop_it.domain.identity_verification.repository.IdentityVerificationRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.util.CryptoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// IdentityVerification의 PII 필드(name/birthDate/phone/ci)가 실제 DB에는 암호화된 채로 저장되고
// JPA로 조회할 때만 복호화되는지, ciHash가 평문 노출 없이 조회/중복판별용으로 쓰일 수 있는지 검증한다.
// CryptoService 자체의 암/복호화·해시 알고리즘 검증은 CryptoServiceTest 참고.
@SpringBootTest
@Transactional
class IdentityVerificationEncryptionTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IdentityVerificationRepository identityVerificationRepository;
    @Autowired
    private CryptoService cryptoService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String NAME = "홍길동";
    private static final String BIRTH_DATE = "1990-01-01";
    private static final String PHONE = "01012345678";
    private static final String CI = "raw-ci-value";

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .socialProvider(SocialProvider.KAKAO)
                .socialUid("identity-verification-encryption-uid")
                .nickname("host")
                .currentMode(UserMode.HOST)
                .build());
    }

    @Test
    @DisplayName("PII 필드는 DB에 평문이 아닌 암호문으로 저장되고, JPA로 조회하면 원문으로 복호화된다")
    void piiFields_areStoredEncryptedInDatabase_andDecryptedOnRead() {
        IdentityVerification saved = identityVerificationRepository.save(IdentityVerification.builder()
                .identityVerificationId("encryption-test-id")
                .status(PortOneVerificationStatus.VERIFIED)
                .name(NAME)
                .gender(Gender.MALE)
                .phone(PHONE)
                .birthDate(BIRTH_DATE)
                .ci(CI)
                .ciHash(cryptoService.hash(CI))
                .user(user)
                .verifiedAt(LocalDateTime.now())
                .build());

        // 영속성 컨텍스트를 비워, 이후 조회가 1차 캐시가 아닌 실제 DB round-trip을 타도록 강제
        entityManager.flush();
        entityManager.clear();

        // JPA 컨버터를 우회하는 네이티브 쿼리로 DB에 실제로 저장된 원시 값을 직접 확인
        Object[] rawRow = (Object[]) entityManager.createNativeQuery(
                        "SELECT name, birth_date, phone, ci FROM identity_verification WHERE id = ?1")
                .setParameter(1, saved.getId())
                .getSingleResult();

        assertThat((String) rawRow[0]).isNotEqualTo(NAME);
        assertThat((String) rawRow[1]).isNotEqualTo(BIRTH_DATE);
        assertThat((String) rawRow[2]).isNotEqualTo(PHONE);
        assertThat((String) rawRow[3]).isNotEqualTo(CI);

        // JPA로 다시 조회하면 @Convert(CryptoConverter)가 자동으로 복호화해 원문 그대로 돌려준다
        IdentityVerification reloaded = identityVerificationRepository.findByUser(user).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo(NAME);
        assertThat(reloaded.getBirthDate()).isEqualTo(BIRTH_DATE);
        assertThat(reloaded.getPhone()).isEqualTo(PHONE);
        assertThat(reloaded.getCi()).isEqualTo(CI);
    }

    @Test
    @DisplayName("ciHash는 평문 CI 없이도 조회/중복판별에 쓸 수 있다")
    void ciHash_canBeUsedForLookupAndDuplicateDetection_withoutDecryptingCi() {
        String ciHash = cryptoService.hash(CI);

        identityVerificationRepository.save(IdentityVerification.builder()
                .identityVerificationId("hash-test-id")
                .status(PortOneVerificationStatus.VERIFIED)
                .name(NAME)
                .gender(Gender.MALE)
                .phone(PHONE)
                .birthDate(BIRTH_DATE)
                .ci(CI)
                .ciHash(ciHash)
                .user(user)
                .verifiedAt(LocalDateTime.now())
                .build());

        entityManager.flush();
        entityManager.clear();

        // findCiHashByUser는 ciHash 컬럼만 조회해, 다른 암호화 필드를 불필요하게 복호화하지 않는다
        assertThat(identityVerificationRepository.findCiHashByUser(user)).contains(ciHash);
        assertThat(identityVerificationRepository.existsByCiHash(ciHash)).isTrue();
        assertThat(identityVerificationRepository.existsByCiHash(cryptoService.hash("다른-ci-값"))).isFalse();
    }
}
