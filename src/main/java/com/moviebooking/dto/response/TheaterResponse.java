package com.moviebooking.dto.response;

public record TheaterResponse(
        Long id,
        String name,
        String city,
        String address
) {}
