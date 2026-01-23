package com.reto.tecnico.customer_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Service API")
                        .version("v1")
                        .description("Customer management endpoints for the banking system."))
                .addTagsItem(new Tag().name("Customer").description("Customer operations"));
    }
}
