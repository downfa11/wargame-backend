package com.ns.common;



import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi OpenApiCommunity() {
        String[] paths = { "/**" };
        return GroupedOpenApi.builder().
                group("OpenApi Swagger v2")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Wargame v1 API")))
                .pathsToMatch(paths)
                .build();
    }

}