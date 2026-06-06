package com.moviebooking.mapper;

import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.dto.response.ShowSeatResponse;
import com.moviebooking.entity.Show;
import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.enums.ShowSeatStatus;

public final class ShowMapper {

    private ShowMapper() {}

    public static ShowResponse toResponse(Show show, long availableSeats, long totalSeats) {
        var screen = show.getScreen();
        var theater = screen.getTheater();
        return new ShowResponse(
                show.getId(),
                show.getMovie().getId(),
                show.getMovie().getTitle(),
                screen.getId(),
                screen.getName(),
                theater.getName(),
                theater.getCity(),
                show.getStartTime(),
                show.getEndTime(),
                show.getBasePrice(),
                show.getLanguage(),
                show.getStatus(),
                availableSeats,
                totalSeats
        );
    }

    public static ShowSeatResponse toShowSeatResponse(ShowSeat showSeat) {
        var seat = showSeat.getSeat();
        return new ShowSeatResponse(
                showSeat.getId(),
                seat.getId(),
                seat.getSeatCode(),
                seat.getRowLabel(),
                seat.getColumnNumber(),
                seat.getSeatType(),
                showSeat.getStatus(),
                showSeat.getPrice()
        );
    }
}
