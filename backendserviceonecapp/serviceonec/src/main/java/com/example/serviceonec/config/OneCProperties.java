package com.example.serviceonec.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "onec.odatafin")
@Getter
@Setter
public class OneCProperties {
    private String baseUrl;
    private String username;
    private String password;
}
