package com.popIt.pop_it.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

@Configuration
public class SwaggerConfig {

    // Swagger UI 태그 노출 순서 지정
    private static final List<String> TAG_ORDER = List.of(
            "인증",
            "사용자",
            "호스트",
            "시설",
            "공간",
            "공간 찜",
            "공간 3D 큐레이션 - 씬",
            "공간 3D 큐레이션 - 핫스팟",
            "예약",
            "본인인증",
            "계약",
            "결제",
            "업로드"
    );

    @Bean
    public OpenAPI swagger() {
        Info info = new Info().title("Pop It").description("단기 상업 공간 대관 플랫폼, 팝잇").version("0.0.1");

        // JWT 토큰 헤더 방식
        String securityScheme = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityScheme);

        Components components = new Components()
                .addSecuritySchemes(securityScheme, new SecurityScheme()
                        .name(securityScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("Bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    // OpenApiCustomizer: 스웨거 문서가 화면에 그려지기 바로 전 단계에서 실행되는 최종 커스텀 필터
    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() == null) {
                return;
            }
            // 태그 순서 정렬
            List<Tag> sorted = openApi.getTags().stream()
                    .sorted(Comparator.comparingInt(tag -> {
                        int index = TAG_ORDER.indexOf(tag.getName());
                        return index == -1 ? Integer.MAX_VALUE : index;
                    }))
                    .toList();
            openApi.setTags(sorted);
        };
    }
}
