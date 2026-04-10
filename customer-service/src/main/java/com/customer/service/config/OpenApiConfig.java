package com.customer.service.config;

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

    @Bean
    public OpenAPI customerRegistrationOpenAPI() {  // lowercase 'c' — method names should be camelCase
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Registration API")
                        .description("Fintech microservices system for customer registration and account management")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bukola")
                                .email("bukola@example.com")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)   // ← add this
                                        .name("Authorization")           // ← add this
                                        .description("Enter your JWT token — do not include 'Bearer ' prefix")));
    }
}