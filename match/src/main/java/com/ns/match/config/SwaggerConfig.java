package com.ns.match.config;



import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi OpenApiCommunity() {
        String[] paths = { "/game/**" };
        return GroupedOpenApi.builder().
                group("match")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Match API")))
                .pathsToMatch(paths)
                .build();
    }


}