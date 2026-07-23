package com.popIt.pop_it.global.config;

import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.exception.CustomAccessDenied;
import com.popIt.pop_it.global.exception.CustomEntryPoint;
import com.popIt.pop_it.global.handler.OAuthSuccessHandler;
import com.popIt.pop_it.global.security.filter.JwtAuthFilter;
import com.popIt.pop_it.global.security.service.CustomOAuthService;
import com.popIt.pop_it.global.security.service.CustomUserDetailsService;
import com.popIt.pop_it.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuthService customOAuthService;
    private final UserRepository userRepository;

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtil, customUserDetailsService);
    }

    @Bean
    public OAuthSuccessHandler oAuthSuccessHandler() {
        return new OAuthSuccessHandler(jwtUtil, userRepository);
    }

    private final String[] allowUris = {
            // swagger 허용
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/api/v1/auth/reissue",
            "/api/v1/payments/webhook",
            "/api/v1/auth/reissue",
            "/api/v1/facilities",
            "/actuator/health"
    };

    // GET이지만 인증이 필요한 경로 (이래 allowGetUris보다 먼저 평가되어야 함)
    private final String[] authenticatedGetUris = {
            "/api/v1/space/my",
            "/api/v1/space/ai-recommended"
    };

    private final String[] allowGetUris = {
            "/api/v1/spaces/*"
    };

    private final String[] publicAPI = {
            "/api/v1/auth/reissue"
    };

    // H2 콘솔 전용 체인 (로컬 개발용): permitAll과 sameOrigin을 이 범위에만 한정
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public SecurityFilterChain h2consoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().permitAll())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
        // OAuth 로그인 전용 체인: 인가 코드 플로우 동안만 세션 사용
        http.csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/api/v1/auth/oauth/**", "/login/oauth2/**")
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(oauth -> oauth
                        // 로그인 시작: /api/v1/auth/oauth/{registrationId}
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/api/v1/auth/oauth"))
                        // 콜백: /api/v1/auth/oauth/callback/{registrationId}
                        .redirectionEndpoint(redirect -> redirect
                                .baseUri("/api/v1/auth/oauth/callback/*"))
                        // Provider 사용자 정보 조회/가입 처리
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuthService))
                        // 로그인 성공 시 JWT 발급 응답
                        .successHandler(oAuthSuccessHandler())
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDenied())
                        .authenticationEntryPoint(customEntryPoint()));

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        // 일반 API 체인: JWT 기반 stateless 인증
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, authenticatedGetUris).authenticated()
                        .requestMatchers(allowUris).permitAll()
                        .requestMatchers(publicAPI).permitAll()
                        .requestMatchers(HttpMethod.GET, allowGetUris).permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDenied())
                        .authenticationEntryPoint(customEntryPoint()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CustomAccessDenied customAccessDenied() {
        return new CustomAccessDenied();
    }

    @Bean
    public CustomEntryPoint customEntryPoint() {
        return new CustomEntryPoint();
    }


}
