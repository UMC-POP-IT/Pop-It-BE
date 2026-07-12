package com.popIt.pop_it.domain.identity_verification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${portone.api-secret-key}")
    private String portoneApiSecretKey;

    @Bean
    public RestClient portoneRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.portone.io")
                .defaultHeader("Authorization", "PortOne " + portoneApiSecretKey)
                .build();
    }
}
