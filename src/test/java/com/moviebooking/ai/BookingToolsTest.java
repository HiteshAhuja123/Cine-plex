package com.moviebooking.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.request.ConfirmBookingRequest;
import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.dto.response.ShowSeatResponse;
import com.moviebooking.entity.enums.BookingStatus;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.entity.enums.ShowSeatStatus;
import com.moviebooking.entity.enums.ShowStatus;
import com.moviebooking.exception.BookingExpiredException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.exception.SeatNotAvailableException;
import com.moviebooking.service.BookingService;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.ShowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingTools @Tool methods")
class BookingToolsTest {

    @Mock private MovieService movieService;
    @Mock private ShowService showService;
    @Mock private BookingService bookingService;

    private BookingTools bookingTools;

    @BeforeEach
    void setUp() {
        // Real ObjectMapper with auto-discovered modules (JavaTimeModule etc.)
        bookingTools = new BookingTools(movieService, showService, bookingService,
                new ObjectMapper().findAndRegisterModules());
    }

    // ─── searchMovies ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchMovies returns JSON containing matching movie fields")
    void searchMovies_returnsJsonMovieList() {
        var movie = new MovieResponse(1L, "Interstellar", "Sci-Fi", "Space epic",
                169, "English", LocalDate.of(2014, 11, 7), null, 8.6);
        when(movieService.searchMovies(eq("sci-fi"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movie)));

        String result = bookingTools.searchMovies("sci-fi");

        assertThat(result).contains("Interstellar", "Sci-Fi", "\"id\":1");
    }

    @Test
    @DisplayName("searchMovies returns error JSON when service throws AppException")
    void searchMovies_returnsErrorJson_onAppException() {
        when(movieService.searchMovies(any(), any()))
                .thenThrow(new ResourceNotFoundException("Movie", 99L));

        String result = bookingTools.searchMovies("unknown");

        assertThat(result).contains("\"error\"");
    }

    // ─── listShows ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("listShows returns JSON containing show fields")
    void listShows_returnsJsonShowList() {
        var show = new ShowResponse(10L, 1L, "Interstellar", 2L, "Screen 1",
                "IMAX Theater", "Mumbai", LocalDateTime.now().plusHours(3),
                LocalDateTime.now().plusHours(6), new BigDecimal("250.00"),
                "English", ShowStatus.ACTIVE, 40L, 50L);
        when(showService.getShowsForMovie(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(show)));

        String result = bookingTools.listShows(1L);

        assertThat(result).contains("\"id\":10", "IMAX Theater", "Interstellar");
    }

    @Test
    @DisplayName("listShows returns error JSON when movie not found")
    void listShows_returnsErrorJson_onAppException() {
        when(showService.getShowsForMovie(any(), any()))
                .thenThrow(new ResourceNotFoundException("Movie", 1L));

        String result = bookingTools.listShows(1L);

        assertThat(result).contains("\"error\"");
    }

    // ─── checkSeatAvailability ───────────────────────────────────────────────

    @Test
    @DisplayName("checkSeatAvailability returns JSON list of available seats")
    void checkSeatAvailability_returnsJsonSeatList() {
        var seat = new ShowSeatResponse(100L, 50L, "A1", "A", 1,
                SeatType.REGULAR, ShowSeatStatus.AVAILABLE, new BigDecimal("250.00"));
        when(showService.getAvailableSeats(10L)).thenReturn(List.of(seat));

        String result = bookingTools.checkSeatAvailability(10L);

        assertThat(result).contains("\"showSeatId\":100", "A1", "REGULAR");
    }

    @Test
    @DisplayName("checkSeatAvailability returns error JSON when show not found")
    void checkSeatAvailability_returnsErrorJson_onAppException() {
        when(showService.getAvailableSeats(any()))
                .thenThrow(new ResourceNotFoundException("Show", 99L));

        String result = bookingTools.checkSeatAvailability(99L);

        assertThat(result).contains("\"error\"");
    }

    // ─── holdSeats ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("holdSeats delegates to BookingService with correct request and returns JSON")
    void holdSeats_delegatesToBookingService_returnsJsonBooking() {
        var booking = heldBooking();
        when(bookingService.holdSeats(any(HoldSeatsRequest.class))).thenReturn(booking);

        String result = bookingTools.holdSeats(42L, 10L, List.of(100L, 101L));

        verify(bookingService).holdSeats(new HoldSeatsRequest(42L, 10L, List.of(100L, 101L)));
        assertThat(result).contains("\"id\":1", "HELD", "\"userId\":42");
    }

    @Test
    @DisplayName("holdSeats returns error JSON when seat is already taken")
    void holdSeats_returnsErrorJson_onSeatNotAvailable() {
        when(bookingService.holdSeats(any()))
                .thenThrow(new SeatNotAvailableException("A1"));

        String result = bookingTools.holdSeats(42L, 10L, List.of(100L));

        assertThat(result).contains("\"error\"", "A1");
    }

    @Test
    @DisplayName("holdSeats returns error JSON when show or user not found")
    void holdSeats_returnsErrorJson_onResourceNotFound() {
        when(bookingService.holdSeats(any()))
                .thenThrow(new ResourceNotFoundException("Show", 10L));

        String result = bookingTools.holdSeats(42L, 10L, List.of(100L));

        assertThat(result).contains("\"error\"");
    }

    // ─── confirmBooking ──────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmBooking delegates to BookingService with correct request and returns JSON")
    void confirmBooking_delegatesToBookingService_returnsJsonBooking() {
        var booking = confirmedBooking();
        when(bookingService.confirmBooking(eq(1L), any(ConfirmBookingRequest.class)))
                .thenReturn(booking);

        String result = bookingTools.confirmBooking(1L, 42L);

        verify(bookingService).confirmBooking(1L, new ConfirmBookingRequest(42L));
        assertThat(result).contains("\"id\":1", "CONFIRMED");
    }

    @Test
    @DisplayName("confirmBooking returns error JSON when hold has expired")
    void confirmBooking_returnsErrorJson_onBookingExpired() {
        when(bookingService.confirmBooking(any(), any()))
                .thenThrow(new BookingExpiredException(1L));

        String result = bookingTools.confirmBooking(1L, 42L);

        assertThat(result).contains("\"error\"");
    }

    @Test
    @DisplayName("confirmBooking returns error JSON when booking not found")
    void confirmBooking_returnsErrorJson_onResourceNotFound() {
        when(bookingService.confirmBooking(any(), any()))
                .thenThrow(new ResourceNotFoundException("Booking", 1L));

        String result = bookingTools.confirmBooking(1L, 42L);

        assertThat(result).contains("\"error\"");
    }

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private BookingResponse heldBooking() {
        return new BookingResponse(1L, 42L, "John Doe", 10L, "Interstellar",
                LocalDateTime.now().plusHours(3), "IMAX Theater",
                BookingStatus.HELD, new BigDecimal("500.00"),
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(5), null, List.of());
    }

    private BookingResponse confirmedBooking() {
        return new BookingResponse(1L, 42L, "John Doe", 10L, "Interstellar",
                LocalDateTime.now().plusHours(3), "IMAX Theater",
                BookingStatus.CONFIRMED, new BigDecimal("500.00"),
                LocalDateTime.now(), null, LocalDateTime.now(), List.of());
    }
}
