package com.moviebooking.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * POST body for /api/assistant.
 *
 * <p>{@code message} is a free-text natural language request.
 * {@code userId} scopes any booking actions to a specific user.
 */
public record AssistantRequest(

        @NotNull(message = "userId is required for booking actions")
        @Positive(message = "userId must be positive")
        Long userId,

        @NotBlank(message = "message cannot be blank")
        String message
) {}
