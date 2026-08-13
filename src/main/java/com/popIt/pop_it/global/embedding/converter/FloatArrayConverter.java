package com.popIt.pop_it.global.embedding.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * pgvector 없이 MySQL/H2에 임베딩 벡터를 저장하기 위한 JSON 직렬화 컨버터.
 */
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("임베딩 벡터 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, float[].class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("임베딩 벡터 역직렬화에 실패했습니다.", e);
        }
    }
}
