package com.moviebooking.exception;

import org.springframework.http.HttpStatus;

/**
 * Root of the application exception hierarchy.
 *
 * <p>Every thrown exception carries an {@link HttpStatus} so that
 * {@link GlobalExceptionHandler} can map it to the correct HTTP response
 * without a giant if-else chain.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public AppException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
