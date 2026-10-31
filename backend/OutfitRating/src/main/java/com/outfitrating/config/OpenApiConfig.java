package com.outfitrating.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI outfitRatingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OutfitRating API")
                        .version("v1")
                        .description("""
                                Community-driven rating platform for outfits and individual garments.
                                Every member scores a look across five criteria — style, fit, colour,
                                originality and quality — which roll up into a 0..100 verdict that
                                drives the weekly and monthly leaderboards.""")
                        .contact(new Contact().name("OutfitRating")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
