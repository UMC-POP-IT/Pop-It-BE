package com.popIt.pop_it.domain.space.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class KakaoLocalRestClientConfig {

    @Value("${kakao.local.rest-api-key}")
    private String kakaoRestApiKey;

    @Bean
    public RestClient kakaoLocalRestClient() {

        // 공간 등록 응답이 카카오 응답 지연에 끌려가지 않도록 짧게 설정
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + kakaoRestApiKey)
                .requestFactory(requestFactory)
                .build();
    }
}
