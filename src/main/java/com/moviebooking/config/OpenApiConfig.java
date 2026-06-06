package com.moviebooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI movieBookingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movie Ticket Booking API")
                        .description("""
                                Production-quality booking backend demonstrating:
                                - DB-level pessimistic locking (seat double-booking prevention)
                                - Strategy pattern for runtime price calculation
                                - Java 21 virtual threads
                                - Agentic AI assistant (Spring AI + Google Gemini)
                                - Paginated, sortable listings with custom JPQL/native queries
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Hitesh Ahuja")
                                .email("hitesh.ahuja2293@gmail.com"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("/").description("Current server")
                ));
    }
}
