package com.popIt.pop_it.global.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// SHA-256 단방향 해시 함수
public class HashUtil {

    private static final int BUFFER_SIZE = 8192;

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

    /**
     * 대용량 파일 등 전체를 메모리에 올리기 부담스러운 경우
     */
    // 전체를 byte[]로 먼저 읽어들이면 파일 크기만큼 메모리를 잡아먹으므로,
    // 스트림을 조금씩 읽어 넘기면서 다이제스트만 갱신한다 (버퍼 크기만큼만 메모리 사용).
    public static String sha256(InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInputStream = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (digestInputStream.read(buffer) != -1) {
                    // 읽는 동안 DigestInputStream이 내부적으로 다이제스트를 갱신한다
                }
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        } catch (IOException e) {
            throw new IllegalStateException("스트림을 읽는 중 오류가 발생했습니다", e);
        }
    }
}
