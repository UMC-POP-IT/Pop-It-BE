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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Duration accessExpiration;


    public JwtUtil(
            @Value("${jwt.token.secretKey}") String secret,
            @Value("${jwt.token.expiration.access}") Long accessExpiration) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofMillis(accessExpiration);
    }

    // AccessToken 생성
    public String createAccessToken(AuthUser user) {
        return createToken(user, accessExpiration);
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

    // 토큰 생성
    public String createToken(AuthUser user, Duration expiration) {
        Instant now = Instant.now();

        // 인가 정보
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(user.getUsername()) // USER UID를 subject로
                .claim("role", authorities)
                .claim("social_provider", user.getUser().getSocialProvider())
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
}
