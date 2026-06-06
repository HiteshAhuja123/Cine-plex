package com.moviebooking.controller;

import com.moviebooking.dto.ApiResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.dto.response.ShowSeatResponse;
import com.moviebooking.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Show browsing and seat availability. */
@RestController
@RequestMapping("/api/shows")
@Tag(name = "Shows", description = "Browse shows and check seat availability")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    /**
     * Get a single show with availability summary.
     */
    @GetMapping("/{showId}")
    @Operation(summary = "Get show details")
    public ResponseEntity<ApiResponse<ShowResponse>> getShow(@PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success(showService.getShowById(showId)));
    }

    /**
     * Get all upcoming shows for a given movie, sorted by start time.
     */
    @GetMapping("/movie/{movieId}")
    @Operation(summary = "List shows for a movie")
    public ResponseEntity<ApiResponse<Page<ShowResponse>>> getShowsForMovie(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());
        return ResponseEntity.ok(ApiResponse.success(showService.getShowsForMovie(movieId, pageable)));
    }

    /**
     * Full seat map for a show — each seat with its current status (AVAILABLE/HELD/BOOKED).
     * This is what a user sees before selecting seats.
     */
    @GetMapping("/{showId}/seats")
    @Operation(summary = "View seat availability for a show",
               description = "Returns all seats with status. AVAILABLE seats can be held.")
    public ResponseEntity<ApiResponse<List<ShowSeatResponse>>> getSeatAvailability(
            @PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success(showService.getSeatAvailability(showId)));
    }
}
