package com.moviebooking.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a seat is HELD or BOOKED at the moment a user tries to hold it.
 * Maps to HTTP 409 Conflict — the seat exists but cannot be reserved right now.
 */
public class SeatNotAvailableException extends AppException {

    public SeatNotAvailableException(String seatCode) {
        super("Seat '" + seatCode + "' is not available for booking", HttpStatus.CONFLICT);
    }

    public SeatNotAvailableException(String message, boolean raw) {
        super(message, HttpStatus.CONFLICT);
    }
}
