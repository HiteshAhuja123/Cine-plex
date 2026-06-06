package com.moviebooking.mapper;

import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.TopMovieResponse;
import com.moviebooking.entity.Movie;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts Movie entities and native-query rows to response DTOs.
 *
 * <p>Demonstrates Java Streams + method references:
 * {@code movies.stream().map(MovieMapper::toResponse).collect(Collectors.toList())}
 */
public final class MovieMapper {

    private MovieMapper() {}

    public static MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getGenre(),
                movie.getDescription(),
                movie.getDurationMinutes(),
                movie.getLanguage(),
                movie.getReleaseDate(),
                movie.getPosterUrl(),
                movie.getAverageRating()
        );
    }

    public static List<MovieResponse> toResponseList(List<Movie> movies) {
        // Method reference — equivalent to m -> MovieMapper.toResponse(m)
        return movies.stream()
                .map(MovieMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Maps a native query row [id, title, genre, posterUrl, bookingCount] to TopMovieResponse. */
    public static TopMovieResponse toTopMovieResponse(Object[] row) {
        return new TopMovieResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                ((Number) row[4]).longValue()
        );
    }
}
