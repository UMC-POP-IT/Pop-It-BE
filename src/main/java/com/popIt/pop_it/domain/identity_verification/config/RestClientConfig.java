package com.popIt.pop_it.domain.identity_verification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;


@Configuration
public class RestClientConfig {

    @Value("${portone.api-secret}")
    private String portoneApiSecretKey;

    @Bean
    public RestClient portoneRestClient() {

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(3)) //포트원 서버와 연결 자체가 안 맺어지는 상황을 빠르게 감지
                .withReadTimeout(Duration.ofSeconds(5)); // 연결은 됐지만 응답이 느린 상황 대비

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return RestClient.builder()
                .baseUrl("https://api.portone.io")
                .defaultHeader("Authorization", "PortOne " + portoneApiSecretKey)
                .requestFactory(requestFactory)
                .build();
    }
}
