package com.capoo.post.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI postOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Post Service API")
                        .description("Posts of the users and the search of the posts of the friends")
                        .version("1.0.0"))
                .servers(List.of(
                        // Relative: resolved against the host the Swagger page was opened from (local or server)
                        new Server().url("/api/v1/post").description("Via API Gateway"),
                        new Server().url("http://localhost:8083/post").description("Direct")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
