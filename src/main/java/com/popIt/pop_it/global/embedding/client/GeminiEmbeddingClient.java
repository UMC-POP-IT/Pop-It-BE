package com.popIt.pop_it.global.embedding.client;

import com.popIt.pop_it.global.embedding.config.GeminiProperties;
import com.popIt.pop_it.global.embedding.dto.GeminiEmbeddingDto.*;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Component
public class GeminiEmbeddingClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiEmbeddingClient(GeminiProperties properties) {
        this.properties = properties;

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(3)) // Gemini 서버와 연결 자체가 안 맺어지는 상황을 빠르게 감지
                .withReadTimeout(Duration.ofSeconds(10)); // 임베딩 추론이라 단순 API보다 느릴 수 있음
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", properties.apiKey())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 순수 텍스트를 임베딩. task prefix는 호출부에서 미리 조합해서 넘긴다.
     */
    public float[] embed(String text) {
        EmbedRequest request = new EmbedRequest(
                new Content(List.of(new Part(text))),
                properties.outputDimensionality()
        );

        EmbedResponse response = restClient.post()
                .uri("/models/{model}:embedContent", properties.model())
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.embedding() == null || response.embedding().values().isEmpty()) {
            throw new IllegalStateException("Gemini 임베딩 응답이 비어있습니다.");
        }

        List<Double> values = response.embedding().values();
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }
        return vector;
    }
}
