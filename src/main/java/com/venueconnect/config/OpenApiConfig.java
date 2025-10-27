package com.venueconnect.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "VenueConnect API", version = "v1", description = "API for Event Ticketing Platform"),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development server")
        }
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // This is the name we'll see in Swagger UI
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // 1. Add a global security requirement
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                // 2. Define the security scheme
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP) // Type is HTTP
                                .scheme("bearer")               // Scheme is "bearer"
                                .bearerFormat("JWT")            // Format is "JWT"
                        )
                );
    }
}