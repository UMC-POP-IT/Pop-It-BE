package com.popIt.pop_it.global.util;

import java.util.List;

/**
 * 임베딩 벡터 연산(코사인 유사도, 가중 평균) 전담 유틸.
 * pgvector 없이 애플리케이션 레벨에서 직접 계산한다. (공간 규모 80개 수준이라 순차 스캔으로 충분)
 */
public final class VectorMath {

    private VectorMath() {}

    public record WeightedVector(float[] vector, double weight) {}

    public static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static float[] weightedAverage(List<WeightedVector> items) {
        int dimension = items.get(0).vector().length;
        double[] sum = new double[dimension];
        double weightSum = 0;

        for (WeightedVector item : items) {
            float[] vector = item.vector();
            double weight = item.weight();
            for (int i = 0; i < dimension; i++) {
                sum[i] += vector[i] * weight;
            }
            weightSum += weight;
        }

        float[] result = new float[dimension];
        if (weightSum == 0) {
            return result;
        }
        for (int i = 0; i < dimension; i++) {
            result[i] = (float) (sum[i] / weightSum);
        }
        return result;
    }
}
