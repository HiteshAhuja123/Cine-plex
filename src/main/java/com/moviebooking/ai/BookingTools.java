package com.moviebooking.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.request.ConfirmBookingRequest;
import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.service.BookingService;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.ShowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI-callable tools for the booking assistant.
 *
 * <p>Each {@code @Tool}-annotated method is a function the LLM can invoke.
 * Spring AI 1.0.0 uses {@link Tool} on methods of a {@link Component} bean,
 * registered via {@link org.springframework.ai.tool.method.MethodToolCallbackProvider}.
 *
 * <p>CRITICAL: every tool delegates to the same transactional service layer used by
 * the REST API. The agent books through the real, concurrency-safe path — never around it.
 */
@Component
public class BookingTools {

    private static final Logger log = LoggerFactory.getLogger(BookingTools.class);

    private final MovieService movieService;
    private final ShowService showService;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    public BookingTools(MovieService movieService,
                        ShowService showService,
                        BookingService bookingService,
                        ObjectMapper objectMapper) {
        this.movieService = movieService;
        this.showService = showService;
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Step 1 — find movies matching the user's request.
     * Called by the LLM before suggesting shows, to ensure only real titles are offered.
     */
    @Tool(description = "Search for movies by keyword (title or genre). Returns matching movies with IDs, titles, genres, and ratings. Always call this first before listing shows.")
    public String searchMovies(String keyword) {
        log.debug("[AI Tool] searchMovies keyword={}", keyword);
        return toJson(movieService.searchMovies(keyword, PageRequest.of(0, 5)).getContent());
    }

    /**
     * Step 2 — get upcoming shows for a movie found in step 1.
     */
    @Tool(description = "List upcoming shows for a movie. Requires a movieId from searchMovies. Returns show IDs, start times, prices, theater name, and available seat count.")
    public String listShows(Long movieId) {
        log.debug("[AI Tool] listShows movieId={}", movieId);
        return toJson(showService.getShowsForMovie(movieId, PageRequest.of(0, 5)).getContent());
    }

    /**
     * Step 3 — get available seats so the LLM can pick real seat IDs.
     */
    @Tool(description = "Get available seats for a show. Returns seat IDs, seat codes (e.g. A1), types (REGULAR/PREMIUM), and prices. Use the showSeatId values when calling holdSeats.")
    public String checkSeatAvailability(Long showId) {
        log.debug("[AI Tool] checkSeatAvailability showId={}", showId);
        return toJson(showService.getAvailableSeats(showId));
    }

    /**
     * Step 4 — hold the seats (goes through pessimistic lock in BookingService).
     *
     * <p>The {@code userId} is passed in the enriched message — the LLM extracts it.
     * The showSeatIds MUST come from checkSeatAvailability (not invented by the LLM).
     */
    @Tool(description = "Hold specific seats for a user. showSeatIds MUST be IDs obtained from checkSeatAvailability — never invent them. Returns a booking with a 5-minute hold window. The bookingId is needed for confirmBooking.")
    public String holdSeats(Long userId, Long showId, List<Long> showSeatIds) {
        log.debug("[AI Tool] holdSeats userId={} showId={} seats={}", userId, showId, showSeatIds);
        return toJson(bookingService.holdSeats(new HoldSeatsRequest(userId, showId, showSeatIds)));
    }

    /**
     * Step 5 — confirm the booking, completing the purchase.
     */
    @Tool(description = "Confirm a held booking to finalise the ticket purchase. Requires bookingId from holdSeats and the userId. Returns the confirmed booking with all seat details.")
    public String confirmBooking(Long bookingId, Long userId) {
        log.debug("[AI Tool] confirmBooking bookingId={} userId={}", bookingId, userId);
        return toJson(bookingService.confirmBooking(bookingId, new ConfirmBookingRequest(userId)));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\": \"serialization failed: " + e.getMessage() + "\"}";
        }
    }
}
