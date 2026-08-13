package com.popIt.pop_it.global.security.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Spring 컨텍스트 없이 생성자에 값을 직접 주입해 검증하는 순수 단위 테스트
class FrontendOriginResolverTest {

    private static final String DEFAULT = "https://popit.co.kr";

    private FrontendOriginResolver resolver() {
        return new FrontendOriginResolver(
                DEFAULT,
                List.of("http://localhost:5173", "http://localhost:3000")
        );
    }

    @Test
    @DisplayName("화이트리스트에 있는 origin은 그대로 반환한다")
    void allowedOrigin_returnedAsIs() {
        assertThat(resolver().resolve("http://localhost:5173")).isEqualTo("http://localhost:5173");
    }

    @Test
    @DisplayName("null이면 기본 frontend-url로 폴백한다")
    void nullOrigin_returnsDefault() {
        assertThat(resolver().resolve(null)).isEqualTo(DEFAULT);
    }

    @Test
    @DisplayName("빈 문자열이면 기본 frontend-url로 폴백한다")
    void blankOrigin_returnsDefault() {
        assertThat(resolver().resolve("   ")).isEqualTo(DEFAULT);
    }

    @Test
    @DisplayName("화이트리스트에 없는 origin은 기본 frontend-url로 폴백한다")
    void unknownOrigin_returnsDefault() {
        assertThat(resolver().resolve("https://evil.com")).isEqualTo(DEFAULT);
    }

    @Test
    @DisplayName("부분 일치 우회(https://popit.co.kr.evil.com)는 exact match가 아니므로 기본값으로 폴백한다")
    void partialMatchBypass_returnsDefault() {
        assertThat(resolver().resolve("https://popit.co.kr.evil.com")).isEqualTo(DEFAULT);
    }

    @Test
    @DisplayName("끝 슬래시가 붙은 값은 정규화 후 exact match되어 허용한다")
    void trailingSlash_normalizedAndAllowed() {
        assertThat(resolver().resolve("http://localhost:5173/")).isEqualTo("http://localhost:5173");
    }

    @Test
    @DisplayName("frontend-url 자체도 항상 허용 목록에 포함된다")
    void frontendUrl_alwaysAllowed() {
        assertThat(resolver().resolve(DEFAULT)).isEqualTo(DEFAULT);
        assertThat(resolver().getAllowedOrigins()).contains(DEFAULT);
    }
}
