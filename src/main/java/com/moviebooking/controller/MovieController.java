package com.moviebooking.controller;

import com.moviebooking.dto.ApiResponse;
import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.TopMovieResponse;
import com.moviebooking.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Movie catalogue endpoints.
 *
 * <p>Demonstrates paginated, sortable listing and an aggregation query
 * (top-booked movies via native SQL).
 */
@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movies", description = "Browse and search the movie catalogue")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Paginated, keyword-searchable movie listing.
     *
     * <p>Supports filtering by title/genre keyword and full Spring Data pagination
     * ({@code page}, {@code size}, {@code sort=title,asc}).
     *
     * @param keyword optional free-text search across title and genre
     * @param page    0-based page number (default 0)
     * @param size    page size (default 10)
     * @param sort    sort field and direction e.g. "averageRating,desc"
     */
    @GetMapping
    @Operation(summary = "Browse movies",
               description = "Keyword search across title and genre with pagination and sorting")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> browseMovies(
            @Parameter(description = "Search keyword (title or genre)")
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "averageRating,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        return ResponseEntity.ok(ApiResponse.success(movieService.searchMovies(keyword, pageable)));
    }

    /**
     * Retrieve a single movie by ID.
     *
     * @param id movie identifier
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovie(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMovieById(id)));
    }

    /**
     * Top N most-booked movies — uses the native SQL aggregation query.
     *
     * @param limit number of results (1–20, default 5)
     */
    @GetMapping("/top-booked")
    @Operation(summary = "Top booked movies",
               description = "Returns movies ranked by confirmed booking count — uses a native SQL aggregation query")
    public ResponseEntity<ApiResponse<List<TopMovieResponse>>> getTopBooked(
            @RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return ResponseEntity.ok(ApiResponse.success(movieService.getTopBookedMovies(safeLimit)));
    }
}
