package com.moviebooking.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity does not exist — maps to HTTP 404. */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
