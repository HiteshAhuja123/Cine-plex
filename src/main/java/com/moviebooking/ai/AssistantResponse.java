package com.moviebooking.ai;

/** Response from the AI assistant endpoint. */
public record AssistantResponse(
        String reply,
        boolean toolsUsed,
        String model
) {}
