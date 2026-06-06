package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Payload for POST /api/bookings/{id}/confirm. */
public record ConfirmBookingRequest(

        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId
) {}
