package com.moviebooking.dto.response;

import com.moviebooking.entity.enums.ShowStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long screenId,
        String screenName,
        String theaterName,
        String theaterCity,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal basePrice,
        String language,
        ShowStatus status,
        long availableSeats,
        long totalSeats
) {}
