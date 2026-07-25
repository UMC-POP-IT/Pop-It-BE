package com.popIt.pop_it.global.security.oauth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth 로그인 성공 후 프론트로 토큰을 안전하게 전달하기 위한 1회용 코드 저장소.
 * 백엔드는 리다이렉트 시 토큰이 아닌 코드만 전달하고, 프론트는 이 코드를
 * /api/v1/auth/exchange 로 교환해 실제 토큰을 받는다.
 * 코드는 짧은 TTL 동안만 유효하며, 조회(consume) 즉시 폐기된다(1회성).
 *
 * 데모 규모(EC2 단일 인스턴스) 기준 in-memory로 구현.
 * 인스턴스가 여러 대로 늘어나면 Redis 등 공유 저장소로 교체 필요.
 */
@Component
public class OAuthCodeStore {

    private static final long TTL_MILLIS = 30_000; // 30초 — 리다이렉트 직후 바로 교환되는 걸 전제로 한 짧은 유효기간

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String issue(String accessToken, String refreshToken) {
        cleanupExpired();
        String code = UUID.randomUUID().toString();
        store.put(code, new Entry(accessToken, refreshToken, Instant.now().plusMillis(TTL_MILLIS)));
        return code;
    }

    /**
     * 코드를 토큰으로 교환한다. 성공/실패 여부와 관계없이 코드는 즉시 제거된다(1회성).
     * @return 유효한 코드였다면 토큰 쌍, 만료되었거나 존재하지 않으면 null
     */
    public TokenPair consume(String code) {
        Entry entry = store.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return null;
        }
        return new TokenPair(entry.accessToken(), entry.refreshToken());
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record Entry(String accessToken, String refreshToken, Instant expiresAt) {}

    public record TokenPair(String accessToken, String refreshToken) {}
}
