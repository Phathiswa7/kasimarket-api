package com.kasimarket.kasimarket_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Settings for the Swagger page (the clickable API documentation).
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI kasiMarketOpenApi() {
        // The name we give to our "login token" security setup.
        String schemeName = "bearerAuth";

        return new OpenAPI()
                // The title and description shown at the top of the page.
                .info(new Info()
                        .title("KasiMarket API")
                        .description("On-demand task marketplace. Log in through /auth/login, "
                                + "copy the token, click Authorize and paste it in.")
                        .version("0.1"))
                // Says: every address needs the token (the open ones simply ignore it).
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                // Describes the token type: a "Bearer" token in JWT format.
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}