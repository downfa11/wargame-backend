package com.ns.membership.config;



import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi OpenApiCommunity() {
        String[] paths = { "/v1/**" };
        return GroupedOpenApi.builder().
                group("membership")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Membership API")))
                .pathsToMatch(paths)
                .build();
    }


}