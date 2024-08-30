package com.ns.wargame.Config;



import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi OpenApiCommunity() {
        String[] paths = { "/v1/**" };
        return GroupedOpenApi.builder().
                group("community")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("v1 API")))
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi OpenApiGame() {
        String[] paths = { "/game/**" };
        return GroupedOpenApi.builder().
                group("game")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("game API")))
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi OpenApiUser() {
        String[] paths = { "/users/**" };
        return GroupedOpenApi.builder().
                group("user")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("user API")))
                .pathsToMatch(paths)
                .build();
    }


}