package com.moviebooking.dto.response;

import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.entity.enums.ShowSeatStatus;

import java.math.BigDecimal;

public record ShowSeatResponse(
        Long showSeatId,
        Long seatId,
        String seatCode,
        String rowLabel,
        Integer columnNumber,
        SeatType seatType,
        ShowSeatStatus status,
        BigDecimal price
) {}
