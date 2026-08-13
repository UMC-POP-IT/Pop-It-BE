package com.popIt.pop_it.global.security.util;

import com.popIt.pop_it.domain.user.entity.enums.SocialProvider;
import com.popIt.pop_it.global.security.entity.AuthUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;
    private final Clock clock;
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private static final int MIN_SECRET_KEY_BYTES = 32;

    public JwtUtil(
            @Value("${jwt.token.secretKey}") String secret,
            @Value("${jwt.token.expiration.access}") Long accessExpiration,
            @Value("${jwt.token.expiration.refresh}") Long refreshExpiration,
            Clock clock) {

        // JWT_SECRET_KEY 최소 길이 검증 로직
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY는 최소 " + MIN_SECRET_KEY_BYTES + "바이트 이상이어야 합니다. "
                            + "현재 길이: " + secretBytes.length + "바이트"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofMillis(accessExpiration);
        this.refreshExpiration = Duration.ofMillis(refreshExpiration);
        this.clock = clock;
    }

    // AccessToken 생성
    public String createAccessToken(AuthUser user) {
        return createToken(user, accessExpiration, ACCESS_TOKEN_TYPE);
    }

    // RefreshToken 생성
    public String createRefreshToken(AuthUser user) {
        return createToken(user, refreshExpiration, REFRESH_TOKEN_TYPE);
    }

    // 토큰에서 소셜 uid 가져오기
    public String getUid(String token) {
        try {
            return getClaims(token).getPayload().getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    // 토큰에서 소셜 로그인 타입 가져오기
    public SocialProvider getSocialProvider(String token) {
        try {
            return SocialProvider.valueOf(getClaims(token).getPayload().get("social_provider").toString().toUpperCase());
        } catch (JwtException e) {
            return null;
        }
    }

    // 토큰 유효성 확인
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException e){
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return isTokenType(token, ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshToken(String token) {
        return isTokenType(token, REFRESH_TOKEN_TYPE);
    }

    // 토큰 생성
    public String createToken(AuthUser user, Duration expiration, String tokenType) {
        Instant now = Instant.now(clock);

        // 인가 정보
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(user.getUsername()) // USER UID를 subject로
                .claim("role", authorities)
                .claim("social_provider", user.getUser().getSocialProvider())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey)
                .compact();
    }

    // 토큰 정보 가져오기
    private Jws<Claims> getClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token);
    }

    private boolean isTokenType(String token, String tokenType) {
        try {
            Object claim = getClaims(token).getPayload().get(TOKEN_TYPE_CLAIM);
            return tokenType.equals(claim);
        } catch (JwtException e) {
            return false;
        }
    }
}
