package com.popIt.pop_it.global.embedding.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.popIt.pop_it.global.embedding.dto.GeminiEmbeddingDto.EmbedResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GeminiEmbeddingDtoTest {

    // Gemini embedContent가 실제로 내려주는 응답 모양 - "embeddings"(복수)가 아니라 "embedding"(단수) 객체
    private static final String REAL_GEMINI_RESPONSE = """
            {
              "embedding": {
                "values": [0.1, 0.2, 0.3]
              },
              "usageMetadata": {
                "promptTokenCount": 8
              }
            }
            """;

    @Test
    void 실제_Gemini_응답_모양을_embedding_단수_필드로_역직렬화한다() {
        JsonMapper mapper = JsonMapper.builder().build();

        EmbedResponse response = mapper.readValue(REAL_GEMINI_RESPONSE, EmbedResponse.class);

        assertThat(response.embedding()).isNotNull();
        assertThat(response.embedding().values()).containsExactly(0.1, 0.2, 0.3);
    }
}
