package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import com.popIt.pop_it.global.embedding.client.GeminiEmbeddingClient;
import com.popIt.pop_it.global.embedding.util.EmbeddingTaskFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공간의 description/category/dong을 조합해 Gemini 임베딩을 생성하고 Space.embedding에 저장한다.
 * SpaceCreatedEvent를 AFTER_COMMIT 시점에 받아 호출된다 (SpaceEmbeddingGenerationListener 참고).
 *
 */
@Service
@RequiredArgsConstructor
public class SpaceEmbeddingService {

    private final SpaceRepository spaceRepository;
    private final GeminiEmbeddingClient geminiEmbeddingClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAndSaveEmbedding(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        String content = (space.getDong() == null || space.getDong().isBlank())
                ? "%s (카테고리: %s)".formatted(space.getDescription(), space.getSpaceCategory().getDescription())
                : "%s (카테고리: %s, 동: %s)".formatted(
                        space.getDescription(), space.getSpaceCategory().getDescription(), space.getDong());
        String document = EmbeddingTaskFormatter.forDocument(content);

        float[] embedding = geminiEmbeddingClient.embed(document);
        space.updateEmbedding(embedding);
        spaceRepository.save(space);
    }
}
