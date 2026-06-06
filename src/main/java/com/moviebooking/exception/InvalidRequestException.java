package com.moviebooking.exception;

import org.springframework.http.HttpStatus;

/** Thrown for business-rule violations that Bean Validation cannot catch (HTTP 400). */
public class InvalidRequestException extends AppException {

    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
