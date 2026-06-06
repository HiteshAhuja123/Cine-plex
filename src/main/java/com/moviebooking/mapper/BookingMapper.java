package com.moviebooking.mapper;

import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.entity.Booking;

import java.util.stream.Collectors;

public final class BookingMapper {

    private BookingMapper() {}

    public static BookingResponse toResponse(Booking booking) {
        var show = booking.getShow();
        var seats = booking.getBookingSeats().stream()
                .map(bs -> ShowMapper.toShowSeatResponse(bs.getShowSeat()))
                .collect(Collectors.toList());

        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getUser().getName(),
                show.getId(),
                show.getMovie().getTitle(),
                show.getStartTime(),
                show.getScreen().getTheater().getName(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                booking.getExpiresAt(),
                booking.getConfirmedAt(),
                seats
        );
    }
}
