package com.moviebooking.dto.response;

/** Projection for the native top-booked-movies aggregation query. */
public record TopMovieResponse(
        Long id,
        String title,
        String genre,
        String posterUrl,
        Long bookingCount
) {}
