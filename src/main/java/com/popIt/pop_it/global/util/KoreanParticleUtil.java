package com.popIt.pop_it.global.util;

/**
 * 한글 단어 끝음절의 받침 유무에 따라 "와/과" 조사를 골라주는 유틸.
 */
public final class KoreanParticleUtil {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    private static final int JONGSEONG_COUNT = 28;

    private KoreanParticleUtil() {}

    /**
     * word 끝음절에 받침이 있으면 "과", 없으면 "와"를 반환한다.
     * 한글 완성형 음절이 아니거나 빈 문자열이면 받침이 있다고 보수적으로 가정해 "과"를 반환한다.
     */
    public static String waGwa(String word) {
        return hasJongseong(word) ? "과" : "와";
    }

    private static boolean hasJongseong(String word) {
        if (word == null || word.isEmpty()) {
            return true;
        }
        char lastChar = word.charAt(word.length() - 1);
        if (lastChar < HANGUL_BASE || lastChar > HANGUL_END) {
            return true;
        }
        return (lastChar - HANGUL_BASE) % JONGSEONG_COUNT != 0;
    }
}
