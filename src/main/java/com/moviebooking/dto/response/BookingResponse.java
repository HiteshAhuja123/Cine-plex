package com.moviebooking.dto.response;

import com.moviebooking.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        Long userId,
        String userName,
        Long showId,
        String movieTitle,
        LocalDateTime showStartTime,
        String theaterName,
        BookingStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime confirmedAt,
        List<ShowSeatResponse> seats
) {}
