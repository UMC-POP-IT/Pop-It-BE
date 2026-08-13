package com.popIt.pop_it.global.util;

import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// AES-GCM 암호화(encrypt/decrypt)와 HMAC-SHA256 해시(hash)만 검증하는 순수 로직 테스트라 Spring 컨텍스트 없이 구성한다.
// secretKey는 @Value로 주입되는 필드라, 실제 설정값과 동일한 형식(Base64로 인코딩된 32바이트 키)을 리플렉션으로 직접 넣어준다.
class CryptoServiceTest {

    // src/test/resources/application.yml의 spring.security.crypto.secret-key와 동일한 값(32바이트)
    private static final String SECRET_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
        ReflectionTestUtils.setField(cryptoService, "secretKey", SECRET_KEY);
    }

    /**
     * encrypt / decrypt
     */

    @Test
    @DisplayName("암호화한 값을 복호화하면 원문과 같다")
    void encryptThenDecrypt_returnsOriginalPlainText() {
        String plainText = "123456-1234567"; // CI/주민등록번호 형태를 가정한 민감정보 예시

        String encrypted = cryptoService.encrypt(plainText);
        String decrypted = cryptoService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("암호화된 값은 원문 그대로 저장되지 않는다")
    void encrypt_doesNotLeakPlainText() {
        String plainText = "홍길동";

        String encrypted = cryptoService.encrypt(plainText);

        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(encrypted).doesNotContain(plainText);
    }

    @Test
    @DisplayName("같은 값을 암호화해도 매번 다른 암호문이 나온다 (IV가 매번 랜덤이라 유니크 제약을 걸 수 없음)")
    void encrypt_sameInput_producesDifferentCipherTextEachTime() {
        String plainText = "01012345678";

        String first = cryptoService.encrypt(plainText);
        String second = cryptoService.encrypt(plainText);

        assertThat(first).isNotEqualTo(second);
        // 하지만 복호화하면 항상 같은 원문으로 돌아온다
        assertThat(cryptoService.decrypt(first)).isEqualTo(plainText);
        assertThat(cryptoService.decrypt(second)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("변조된 암호문을 복호화하려 하면 인증 실패로 예외가 발생한다")
    void decrypt_tamperedCipherText_throwsException() {
        String encrypted = cryptoService.encrypt("민감정보");

        // Base64 디코딩 후 마지막 바이트를 뒤집어 GCM 인증 태그를 깨뜨린다 (변조 재현)
        byte[] bytes = java.util.Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 0xFF;
        String tampered = java.util.Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> cryptoService.decrypt(tampered))
                .isInstanceOf(ProjectException.class);
    }

    /**
     * hash
     */

    @Test
    @DisplayName("같은 값을 해시하면 항상 같은 결과가 나온다 (중복 판별용이므로 결정적이어야 함)")
    void hash_sameInput_isDeterministic() {
        String plainText = "same-ci-value";

        assertThat(cryptoService.hash(plainText)).isEqualTo(cryptoService.hash(plainText));
    }

    @Test
    @DisplayName("다른 값을 해시하면 다른 결과가 나온다")
    void hash_differentInput_producesDifferentHash() {
        assertThat(cryptoService.hash("ci-value-1")).isNotEqualTo(cryptoService.hash("ci-value-2"));
    }

    @Test
    @DisplayName("해시값에는 원문이 그대로 노출되지 않는다")
    void hash_doesNotLeakPlainText() {
        String plainText = "990101-1234567";

        String hashed = cryptoService.hash(plainText);

        assertThat(hashed).isNotEqualTo(plainText);
        assertThat(hashed).doesNotContain(plainText);
    }

    @Test
    @DisplayName("암호화와 달리 해시는 결정적이므로 매번 같은 값을 유니크 판별에 사용할 수 있다")
    void hash_isStableAcrossMultipleCalls_unlikeEncrypt() {
        String plainText = "duplicate-check-target";

        String hash1 = cryptoService.hash(plainText);
        String hash2 = cryptoService.hash(plainText);
        String encrypted1 = cryptoService.encrypt(plainText);
        String encrypted2 = cryptoService.encrypt(plainText);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }
}
