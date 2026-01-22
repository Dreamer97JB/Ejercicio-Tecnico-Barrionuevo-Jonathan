package com.reto.tecnico.account_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .version("v1")
                        .description("Account, movement, and report endpoints for the banking system."))
                .addTagsItem(new Tag().name("Account").description("Account operations"))
                .addTagsItem(new Tag().name("Movement").description("Movement operations"))
                .addTagsItem(new Tag().name("Report").description("Report operations"));
    }
}
