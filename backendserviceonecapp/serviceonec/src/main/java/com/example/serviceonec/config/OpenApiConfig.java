package com.example.serviceonec.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    /**
     * Основное API для UI разработчиков
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("nomenclature-api")
                .displayName("Nomenclature API (для фронтенда)")
                .pathsToMatch("/api/v1/categories/**", "/api/v1/nomenclature/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API")
                        .version("1.0")
                        .description("API для тестирование запросов к сервису!"));
    }

}
