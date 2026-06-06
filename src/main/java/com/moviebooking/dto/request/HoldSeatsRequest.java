package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Payload for POST /api/bookings/hold.
 *
 * <p>Uses a Java record — immutable, compact, no boilerplate.
 * Bean Validation annotations are placed on record components.
 */
public record HoldSeatsRequest(

        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId,

        @NotNull(message = "showId is required")
        @Positive(message = "showId must be positive")
        Long showId,

        @NotNull(message = "showSeatIds is required")
        @NotEmpty(message = "At least one seat must be selected")
        List<@Positive Long> showSeatIds
) {}
