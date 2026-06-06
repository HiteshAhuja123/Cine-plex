package com.moviebooking.dto.response;

import java.time.LocalDate;

public record MovieResponse(
        Long id,
        String title,
        String genre,
        String description,
        Integer durationMinutes,
        String language,
        LocalDate releaseDate,
        String posterUrl,
        Double averageRating
) {}
