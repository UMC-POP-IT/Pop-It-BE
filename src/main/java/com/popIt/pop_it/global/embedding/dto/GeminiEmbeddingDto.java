package com.popIt.pop_it.global.embedding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GeminiEmbeddingDto {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EmbedRequest(
            Content content,
            @JsonProperty("output_dimensionality") Integer outputDimensionality
    ) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    // Gemini embedContent 응답은 "embeddings"(복수) 배열이 아니라 "embedding"(단수) 객체 하나로 내려온다
    public record EmbedResponse(Embedding embedding) {}

    public record Embedding(List<Double> values) {}
}