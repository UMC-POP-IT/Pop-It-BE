package com.popIt.pop_it.global.security.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 소셜 로그인 성공 후 프론트로 돌려보낼 최종 목적지 origin을 결정하는 컴포넌트.
 *
 * 왜 필요한가: 목적지가 단일 값(app.frontend-url)으로 고정돼 있으면, 로컬 프론트(localhost)에서
 * 배포 백엔드로 로그인을 시작해도 항상 배포 프론트로 튕겨 로컬 개발이 불가능하다.
 * 프론트가 verifier를 "로그인을 시작한 origin"의 sessionStorage에 저장하므로(PKCE),
 * 최종 리다이렉트가 시작 origin과 달라지면 토큰 교환 자체가 구조적으로 불가능하기 때문이다.
 *
 * 보안: Open Redirect를 만들지 않기 위해, 요청 origin은 화이트리스트와 "정확 일치(exact match)"할 때만 허용한다.
 * startsWith/contains/부분 정규식은 https://popit.co.kr.evil.com 같은 우회를 허용하므로 쓰지 않는다.
 * 화이트리스트에 없거나 비어 있으면 항상 기본값(app.frontend-url)으로 폴백해 기존 프로덕션 동작을 보존한다.
 */
@Component
public class FrontendOriginResolver {

    // 정규화된 기본 목적지. 화이트리스트에 없거나 origin이 없을 때의 폴백 (기존 프로덕션 동작).
    private final String defaultFrontendUrl;

    // 정규화된 허용 origin 집합. frontend-url을 항상 포함한다. CORS 설정과 단일 출처로 공유한다.
    private final Set<String> allowedOrigins;

    public FrontendOriginResolver(
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.allowed-frontend-origins}") List<String> allowedFrontendOrigins
    ) {
        this.defaultFrontendUrl = normalize(frontendUrl);

        // frontend-url은 항상 허용에 포함하고, 그 위에 화이트리스트를 정규화해 합친다.
        Set<String> origins = new LinkedHashSet<>();
        origins.add(this.defaultFrontendUrl);
        for (String origin : allowedFrontendOrigins) {
            String normalized = normalize(origin);
            if (normalized != null && !normalized.isBlank()) {
                origins.add(normalized);
            }
        }
        this.allowedOrigins = Set.copyOf(origins);
    }

    /**
     * 요청 origin이 화이트리스트에 정확히 일치하면 그 값을, 그 외/null/blank면 기본 frontend-url을 반환한다.
     */
    public String resolve(String requestedOrigin) {
        if (requestedOrigin == null || requestedOrigin.isBlank()) {
            return defaultFrontendUrl;
        }
        String normalized = normalize(requestedOrigin);
        return allowedOrigins.contains(normalized) ? normalized : defaultFrontendUrl;
    }

    /**
     * 허용 origin 집합(불변). CORS 설정에서 목적지 허용 목록과 갈라지지 않도록 재사용한다.
     */
    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    // 정규화: 앞뒤 공백 제거 + 끝 슬래시 제거. 정규화한 값끼리 exact match로 비교한다.
    private static String normalize(String origin) {
        if (origin == null) {
            return null;
        }
        String trimmed = origin.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
