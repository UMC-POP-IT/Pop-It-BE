package com.popIt.pop_it.global.security.oauth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth 로그인 성공 후 프론트로 토큰을 안전하게 전달하기 위한 1회용 코드 저장소.
 * 백엔드는 리다이렉트 시 토큰이 아닌 코드만 전달하고, 프론트는 이 코드를
 * /api/v1/auth/exchange 로 교환해 실제 토큰을 받는다.
 * 코드는 짧은 TTL 동안만 유효하며, 조회(consume) 즉시 폐기된다(1회성).
 *
 * PKCE 유사 보호: 로그인 시작 시 프론트가 넘긴 challenge(=SHA256(verifier))를 코드에 바인딩해두고,
 * exchange 시 프론트가 보낸 verifier의 해시가 일치할 때만 토큰을 내준다.
 * 이렇게 하면 code가 URL/히스토리를 통해 유출되더라도, verifier(프론트 로컬에만 존재)를
 * 모르는 제3자는 토큰을 교환할 수 없다.
 *
 * 데모 규모(EC2 단일 인스턴스) 기준 in-memory로 구현.
 * 인스턴스가 여러 대로 늘어나면 Redis 등 공유 저장소로 교체 필요.
 */
@Component
public class OAuthCodeStore {

    private static final long TTL_MILLIS = 30_000; // 30초 — 리다이렉트 직후 바로 교환되는 걸 전제로 한 짧은 유효기간

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * @param challenge 로그인 시작 시 프론트가 넘긴 값 (SHA256(verifier)를 base64url로 인코딩한 것).
     *                  이 값이 없으면 exchange가 항상 실패하므로, 호출부(OAuthSuccessHandler)에서
     *                  challenge가 없을 땐 아예 issue()를 호출하지 않고 별도로 처리한다(PKCE 필수 정책).
     */
    public String issue(String accessToken, String refreshToken, String challenge) {
        cleanupExpired();
        String code = UUID.randomUUID().toString();
        store.put(code, new Entry(accessToken, refreshToken, challenge, Instant.now().plusMillis(TTL_MILLIS)));
        return code;
    }

    /**
     * 코드를 토큰으로 교환한다. 성공/실패 여부와 관계없이 코드는 즉시 제거된다(1회성, 재사용/브루트포스 방지).
     * @param verifier 프론트가 로그인 시작 시 생성해뒀던 원본 값. 발급 시 저장된 challenge와 해시가 일치해야 함.
     * @return 유효했다면 토큰 쌍, 만료/미존재/verifier 불일치면 null
     */
    public TokenPair consume(String code, String verifier) {
        Entry entry = store.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return null;
        }
        if (!matches(entry.challenge(), verifier)) {
            return null;
        }
        return new TokenPair(entry.accessToken(), entry.refreshToken());
    }

    private boolean matches(String challenge, String verifier) {
        if (challenge == null || verifier == null) {
            return false;
        }
        String computed = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sha256(verifier));
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                challenge.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record Entry(String accessToken, String refreshToken, String challenge, Instant expiresAt) {}

    public record TokenPair(String accessToken, String refreshToken) {}
}
