package com.example.serviceonec.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация RestClient для работы с 1С OData API
 *
 * @author Adilhan
 * @version 1.0
 */
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final OneCProperties oneCProperties;

    /**
     * Создает и настраивает RestClient для работы с 1С OData
     *
     * @return настроенный экземпляр RestClient
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(oneCProperties.getBaseUrl())
                .defaultHeader("Authorization", getBasicAuthHeader())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    private String getBasicAuthHeader() {
        String auth = oneCProperties.getUsername()
                + ":"
                + oneCProperties.getPassword();
        return "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes());
    }
}

