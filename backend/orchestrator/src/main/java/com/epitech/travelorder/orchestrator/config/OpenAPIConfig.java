package com.epitech.travelorder.orchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Value("${spring.application.version}")
    private String appVersion;

    @Bean
    public OpenAPI travelOrderAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Travel Order API")
                        .description("REST API for managing travel orders")
                        .version(appVersion)
                        .contact(new Contact()
                                .name("LIL_1-T-AIA911")
                                .email("arthur2504azoula@gmail.com")
                        )
                );
    }
}
