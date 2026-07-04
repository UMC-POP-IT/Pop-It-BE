package com.popIt.pop_it.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
public class SwaggerConfig {

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
}
