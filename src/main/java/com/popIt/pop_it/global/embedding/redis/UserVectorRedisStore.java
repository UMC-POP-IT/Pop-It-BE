package com.popIt.pop_it.global.embedding.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 유저 취향 벡터 캐시를 Redis에 읽고 쓰는 책임만 담당한다.
 */
@Component
@RequiredArgsConstructor
public class UserVectorRedisStore {

    private static final String KEY_PREFIX = "user:vector:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 활동이 끊긴 유저의 캐시가 영구히 남아있지 않도록 TTL을 둔다.
    // (DB를 리셋해도 Redis는 별도로 안 지워지는 경우, userId 재사용 시 옛날 유저의 벡터를
    //  전혀 다른 새 유저가 이어받는 걸 방지하는 안전장치이기도 하다 - recomputeUserVector의 명시적 delete와 별개로)
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, float[] vector) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(vector);
            redisTemplate.opsForValue().set(KEY_PREFIX + userId, json, TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("유저 벡터 직렬화에 실패했습니다.", e);
        }
    }

    public Optional<float[]> find(Long userId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readValue(json, float[].class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("유저 벡터 역직렬화에 실패했습니다.", e);
        }
    }

    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
