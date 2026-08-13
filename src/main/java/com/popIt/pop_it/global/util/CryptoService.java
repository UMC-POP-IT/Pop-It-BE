package com.popIt.pop_it.global.util;

import com.popIt.pop_it.global.apiPayload.code.CryptoErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 12바이트 권장
    private static final int GCM_TAG_LENGTH = 128; // 비트 단위
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${spring.security.crypto.secret-key}")
    private String secretKey; // 32바이트(256bit) 길이의 키 필요

    // AES-GCM 암호화
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(secretKey), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호문을 합쳐서 하나의 문자열로 저장 (복호화 시 IV가 필요하기 때문)
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new ProjectException(CryptoErrorCode.ENCRYPTION_FAILED, e);
        }
    }

    // AES-GCM 복호화
    public String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(secretKey), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ProjectException(CryptoErrorCode.DECRYPTION_FAILED, e);
        }
    }

    // 결정적 단방향 해시(HMAC-SHA256)
    // 암호화 컬럼은 매번 다른 암호문이 나와 유니크 제약을 걸 수 없으므로,
    // 같은 값이면 항상 같은 결과가 나오는 이 해시를 별도 컬럼에 저장해 유니크 판별에 사용한다.
    // 비밀키 기반이라 해시만으로는 원문(예: 10자리 사업자번호)을 무차별 대입으로 역추적하기 어렵다.
    public String hash(String plainText) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(secretKey), HMAC_ALGORITHM));
            byte[] result = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new ProjectException(CryptoErrorCode.ENCRYPTION_FAILED, e);
        }
    }
}
