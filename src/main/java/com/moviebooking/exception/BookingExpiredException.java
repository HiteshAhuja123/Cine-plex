package com.moviebooking.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user tries to confirm a booking whose 5-minute hold window has elapsed. */
public class BookingExpiredException extends AppException {

    public BookingExpiredException(Long bookingId) {
        super("Booking " + bookingId + " has expired. Please start a new reservation.", HttpStatus.GONE);
    }
}
