package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.space.dto.KakaoLocalResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLocalService {

    private final RestClient kakaoLocalRestClient;

    // "성수동1가" -> "성수동"
    private static final Pattern DONG_WITH_GA = Pattern.compile("^(.+동)\\d+가$");
    // "역삼1동", "성수1가제1동" -> "역삼동", "성수동"
    private static final Pattern DONG_WITH_NUMBER = Pattern.compile("^([가-힣]+?)\\d.*동$");

    public Optional<String> resolveDong(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        try {
            KakaoLocalResDTO.RegionCode response = kakaoLocalRestClient.get()
                    .uri(urlBuilder -> urlBuilder
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .build())
                    .retrieve()
                    .body(KakaoLocalResDTO.RegionCode.class);

            if (response == null || response.documents() == null || response.documents().isEmpty()) {
                return Optional.empty();
            }

            return response.documents().stream()
                    .filter(document -> "B".equals(document.regionType()))
                    .findFirst()
                    .or(() -> response.documents().stream().findFirst())
                    .map(document -> document.region3DepthName())
                    .map(dong -> normalizeDong(dong))
                    .filter(dong -> dong != null && !dong.isBlank());

        } catch (Exception e) {
            log.warn("카카오 로컬 API 행정동 변환 실패. latitude={}, longitude={}", latitude, longitude, e);
            return Optional.empty();
        }
    }

    // 법전동, 행정동의 표기를 상권 느낌의 동 이름으로 다듬기
    // ex) 성수동1가 -> 성수동
    private static String normalizeDong(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }

        Matcher getMatcher = DONG_WITH_GA.matcher(rawName);
        if (getMatcher.matches()) {
            return getMatcher.group(1);
        }

        Matcher numberMatch = DONG_WITH_NUMBER.matcher(rawName);
        if (numberMatch.matches()) {
            return numberMatch.group(1) + "동";
        }

        return rawName;
    }
}
