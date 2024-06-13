package com.ns.wargame.Config;



import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi OpenApiCommunity(@Value("${springdoc.version}") String appVersion) {
        String[] paths = { "/v1/**" };
        return GroupedOpenApi.builder().
                group("community")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("v1 API").version(appVersion)))
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi OpenApiGame(@Value("${springdoc.version}") String appVersion) {
        String[] paths = { "/game/**" };
        return GroupedOpenApi.builder().
                group("game")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("game API").version(appVersion)))
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    public GroupedOpenApi OpenApiUser(@Value("${springdoc.version}") String appVersion) {
        String[] paths = { "/users/**" };
        return GroupedOpenApi.builder().
                group("user")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("user API").version(appVersion)))
                .pathsToMatch(paths)
                .build();
    }


}