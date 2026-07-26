package com.popIt.pop_it.global.embedding.util;

public final class EmbeddingTaskFormatter {

    private EmbeddingTaskFormatter() {}

    /**
     * 공간(임대 매물) 설명을 문서로 임베딩할 때.
     * buildingName은 취향 유사도와 무관한 노이즈라 벡터에 안 실리도록 title은 항상 고정값을 쓴다.
     */
    public static String forDocument(String content) {
        return "title: none | text: %s".formatted(content);
    }

    /** 유저 검색어/취향 벡터 생성 시 쿼리로 임베딩할 때 */
    public static String forSearchQuery(String queryText) {
        return "task: search result | query: %s".formatted(queryText);
    }
}
