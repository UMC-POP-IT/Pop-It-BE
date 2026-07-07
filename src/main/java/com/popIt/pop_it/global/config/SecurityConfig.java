package com.popIt.pop_it.global.config;

import com.popIt.pop_it.global.exception.CustomAccessDenied;
import com.popIt.pop_it.global.exception.CustomEntryPoint;
import com.popIt.pop_it.global.handler.OAuthSuccessHandler;
import com.popIt.pop_it.global.security.filter.JwtAuthFilter;
import com.popIt.pop_it.global.security.service.CustomOAuthService;
import com.popIt.pop_it.global.security.service.CustomUserDetailsService;
import com.popIt.pop_it.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtil, customUserDetailsService);
    }

    @Bean
    public OAuthSuccessHandler oAuthSuccessHandler() {
        return new OAuthSuccessHandler(jwtUtil);
    }

    private final String[] allowUris = {
            // swagger 허용
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/auth/**",
    };

    private final String[] publicAPI = {
            "/auth/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                // URI 허용 여부
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(allowUris).permitAll()
                        .requestMatchers(publicAPI).permitAll()
                        .anyRequest().authenticated())
                // 세션
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // JWT 필터
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                // oauth // TODO: 로그인 구현 시 활성화 필요
//                .oauth2Login(oauth -> oauth
//                        // 인증 엔트리 포인트
//                        .authorizationEndpoint(auth -> auth
//                                .baseUri("/oauth/authorize"))
//                        // 콜백 주소
//                        .redirectionEndpoint(redirect -> redirect
//                                .baseUri("/oauth/callback/**"))
//                        // 인증 완료 후 정보 활용
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuthService))
//                        // 성공 시 JWT 토큰 발행할 핸들러
//                        .successHandler(oAuthSuccessHandler())
//                )
                // 예외 상황 핸들러
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
