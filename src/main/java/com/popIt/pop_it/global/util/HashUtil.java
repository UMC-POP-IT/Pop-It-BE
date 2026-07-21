package com.popIt.pop_it.global.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// SHA-256 단방향 해시 함수
public class HashUtil {

    /**
     * 일반 문자열인 경우
     */
    public static String sha256(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 이미지, 바이너리 원본인 경우
     */
    // 이미지 등 바이너리 원본을 해시할 때는 문자열로 변환하지 말고 바이트를 직접 넘겨야 합니다.
    // UTF-8은 임의의 바이트 시퀀스를 손실 없이 표현하지 못해, 문자열로 변환하는 과정에서 원본이 손상될 수 있습니다.
    public static String sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input);
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
