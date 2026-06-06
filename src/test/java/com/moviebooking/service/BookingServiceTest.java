package com.moviebooking.service;

import com.moviebooking.dto.request.ConfirmBookingRequest;
import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.entity.*;
import com.moviebooking.entity.enums.BookingStatus;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.entity.enums.ShowSeatStatus;
import com.moviebooking.exception.BookingExpiredException;
import com.moviebooking.exception.SeatNotAvailableException;
import com.moviebooking.pricing.PricingStrategyFactory;
import com.moviebooking.pricing.WeekdayPricingStrategy;
import com.moviebooking.pricing.WeekendPricingStrategy;
import com.moviebooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService using Mockito.
 *
 * <p>Tests business-logic paths (validation, status transitions, expiry) without
 * needing a database. The ConcurrentBookingTest handles the locking proof.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService unit tests")
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ShowSeatRepository showSeatRepository;
    @Mock private ShowRepository showRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookingSeatRepository bookingSeatRepository;

    private BookingService bookingService;
    private PricingStrategyFactory pricingStrategyFactory;

    private User user;
    private Movie movie;
    private Theater theater;
    private Screen screen;
    private Seat seat;
    private Show show;
    private ShowSeat showSeat;

    @BeforeEach
    void setUp() {
        pricingStrategyFactory = new PricingStrategyFactory(
                new WeekdayPricingStrategy(), new WeekendPricingStrategy());

        bookingService = new BookingService(
                bookingRepository, showSeatRepository, showRepository,
                userRepository, bookingSeatRepository, pricingStrategyFactory);

        // Build a consistent entity graph
        user = new User("Alice", "alice@test.com", null);
        setId(user, 1L);

        movie = new Movie("Test Movie", "DRAMA", "desc", 120,
                "English", LocalDate.now(), null, 7.0);
        setId(movie, 1L);

        theater = new Theater("Cineplex", "NYC", "1 Broadway");
        setId(theater, 1L);

        screen = new Screen(theater, "Screen 1", 10);
        setId(screen, 1L);

        seat = new Seat(screen, "A", 1, SeatType.REGULAR);
        setId(seat, 1L);

        // Use a weekday show time for deterministic pricing
        LocalDateTime monday = LocalDateTime.now()
                .with(java.time.DayOfWeek.MONDAY)
                .plusWeeks(1)
                .withHour(18).withMinute(0);
        show = new Show(movie, screen, monday, monday.plusHours(2),
                new BigDecimal("12.00"), "English");
        setId(show, 1L);

        showSeat = new ShowSeat(show, seat, new BigDecimal("12.00"));
        setId(showSeat, 1L);
    }

    // ── holdSeats tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("holdSeats: happy path creates a HELD booking with correct price")
    void holdSeats_happyPath_createsHeldBooking() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(showSeatRepository.findByIdsWithPessimisticLock(List.of(1L)))
                .thenReturn(List.of(showSeat));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            setId(b, 42L);
            return b;
        });
        when(bookingSeatRepository.saveAll(any())).thenReturn(List.of());

        BookingResponse response = bookingService.holdSeats(
                new HoldSeatsRequest(1L, 1L, List.of(1L)));

        assertThat(response.status()).isEqualTo(BookingStatus.HELD);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);
        assertThat(showSeat.getHeldAt()).isNotNull();
    }

    @Test
    @DisplayName("holdSeats: throws SeatNotAvailableException when seat is already HELD")
    void holdSeats_seatAlreadyHeld_throws() {
        showSeat.setStatus(ShowSeatStatus.HELD);
        showSeat.setHeldAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(showSeatRepository.findByIdsWithPessimisticLock(List.of(1L)))
                .thenReturn(List.of(showSeat));

        assertThatThrownBy(() ->
                bookingService.holdSeats(new HoldSeatsRequest(1L, 1L, List.of(1L))))
                .isInstanceOf(SeatNotAvailableException.class)
                .hasMessageContaining("A1");
    }

    @Test
    @DisplayName("holdSeats: throws SeatNotAvailableException when seat is BOOKED")
    void holdSeats_seatBooked_throws() {
        showSeat.setStatus(ShowSeatStatus.BOOKED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(showSeatRepository.findByIdsWithPessimisticLock(List.of(1L)))
                .thenReturn(List.of(showSeat));

        assertThatThrownBy(() ->
                bookingService.holdSeats(new HoldSeatsRequest(1L, 1L, List.of(1L))))
                .isInstanceOf(SeatNotAvailableException.class);
    }

    // ── confirmBooking tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("confirmBooking: transitions HELD → CONFIRMED before expiry")
    void confirmBooking_beforeExpiry_confirms() {
        Booking booking = new Booking(user, show, new BigDecimal("12.00"),
                LocalDateTime.now().plusMinutes(3)); // still 3 min left
        setId(booking, 10L);
        BookingSeat bs = new BookingSeat(booking, showSeat);
        setId(bs, 100L);
        booking.setBookingSeats(List.of(bs));
        showSeat.setStatus(ShowSeatStatus.HELD);

        when(bookingRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.confirmBooking(
                10L, new ConfirmBookingRequest(1L));

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED);
    }

    @Test
    @DisplayName("confirmBooking: throws BookingExpiredException after TTL")
    void confirmBooking_afterExpiry_throwsExpired() {
        Booking booking = new Booking(user, show, new BigDecimal("12.00"),
                LocalDateTime.now().minusMinutes(1)); // expired 1 min ago
        setId(booking, 10L);
        booking.setBookingSeats(List.of());

        when(bookingRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.confirmBooking(10L, new ConfirmBookingRequest(1L)))
                .isInstanceOf(BookingExpiredException.class)
                .hasMessageContaining("expired");
    }

    // ── cancelBooking tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("cancelBooking: releases seat back to AVAILABLE")
    void cancelBooking_releasesSeats() {
        showSeat.setStatus(ShowSeatStatus.HELD);
        showSeat.setHeldAt(LocalDateTime.now());

        Booking booking = new Booking(user, show, new BigDecimal("12.00"),
                LocalDateTime.now().plusMinutes(4));
        setId(booking, 20L);
        BookingSeat bs = new BookingSeat(booking, showSeat);
        booking.setBookingSeats(List.of(bs));

        when(bookingRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(20L, 1L);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);
        assertThat(showSeat.getHeldAt()).isNull();
    }

    // ── Pricing strategy tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Weekday regular seat: price equals base price")
    void pricing_weekdayRegular_equalsBasePrice() {
        BigDecimal price = pricingStrategyFactory
                .getStrategy(show, seat)
                .calculatePrice(new BigDecimal("12.00"), show, seat);
        assertThat(price).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("Premium seat on weekday: price is 1.5× base price")
    void pricing_weekdayPremium_isOneAndHalfX() {
        seat.setSeatType(SeatType.PREMIUM);
        BigDecimal price = pricingStrategyFactory
                .getStrategy(show, seat)
                .calculatePrice(new BigDecimal("12.00"), show, seat);
        assertThat(price).isEqualByComparingTo(new BigDecimal("18.00"));
    }

    // ── Reflective helper for setting IDs in unit tests ──────────────────────

    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}
