package com.moviebooking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Generic HTTP response envelope used by every endpoint.
 *
 * <p>Demonstrates Generics: a single type-safe wrapper that works for
 * {@code ApiResponse<MovieResponse>}, {@code ApiResponse<BookingResponse>}, etc.
 * The {@code <T>} is unconstrained so it composes with any DTO.
 *
 * <p>Example success JSON:
 * <pre>{@code
 * { "success": true, "data": { ... }, "timestamp": "2024-06-01T10:00:00" }
 * }</pre>
 *
 * @param <T> the type of the {@code data} payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        LocalDateTime timestamp
) {

    /** Convenience factory for successful responses with a payload. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    /** Convenience factory for successful responses with a message and no payload. */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now());
    }

    /** Convenience factory for error responses. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now());
    }
}
