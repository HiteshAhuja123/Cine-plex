package com.moviebooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Movie Ticket Booking System — Spring Boot 3.3 / Java 21.
 *
 * <p>Key design pillars demonstrated here:
 * <ul>
 *   <li>Pessimistic DB-level locking to prevent double-booking (BookingService)</li>
 *   <li>Strategy pattern for runtime price calculation (pricing package)</li>
 *   <li>Virtual threads enabled globally via spring.threads.virtual.enabled=true</li>
 *   <li>Agentic AI assistant using Spring AI tool-calling (ai package)</li>
 * </ul>
 *
 * @see com.moviebooking.service.BookingService
 * @see com.moviebooking.pricing.PricingStrategy
 * @see com.moviebooking.ai.BookingAssistantService
 */
@SpringBootApplication
@EnableScheduling
public class MovieBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieBookingApplication.class, args);
    }
}
