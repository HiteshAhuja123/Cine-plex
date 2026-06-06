package com.moviebooking.controller;

import com.moviebooking.dto.ApiResponse;
import com.moviebooking.dto.request.ConfirmBookingRequest;
import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Booking lifecycle endpoints: hold → confirm → cancel.
 *
 * <p>All mutating operations flow through {@link BookingService} which owns
 * the transactional and concurrency logic — the controller stays thin.
 */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Hold, confirm, cancel, and view bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Step 1 — Hold seats (5-minute TTL).
     *
     * <p>Acquires pessimistic DB locks; exactly one concurrent request per seat wins.
     * Returns 409 if any requested seat is already held or booked.
     */
    @PostMapping("/hold")
    @Operation(summary = "Hold seats",
               description = "Reserves seats with a 5-minute hold. Concurrent requests for the same seat return 409.")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(
            @Valid @RequestBody HoldSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(bookingService.holdSeats(request),
                        "Seats held. Confirm within 5 minutes."));
    }

    /**
     * Step 2 — Confirm a held booking.
     *
     * <p>Returns 410 Gone if the hold has expired.
     */
    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm a held booking",
               description = "Transitions booking from HELD → CONFIRMED. Returns 410 if the 5-minute window has passed.")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody ConfirmBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.confirmBooking(bookingId, request), "Booking confirmed!"));
    }

    /**
     * Cancel a booking — works for HELD and CONFIRMED bookings.
     * Releases seats back to AVAILABLE if previously HELD.
     */
    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.cancelBooking(bookingId, userId), "Booking cancelled."));
    }

    /**
     * Get a single booking — scoped to the requesting user.
     */
    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingById(bookingId, userId)));
    }

    /**
     * Paginated booking history for a user, sorted newest-first.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's booking history")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getUserBookings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getUserBookings(userId, pageable)));
    }
}
