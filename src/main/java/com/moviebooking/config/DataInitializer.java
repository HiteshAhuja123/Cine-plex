package com.moviebooking.config;

import com.moviebooking.entity.*;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.pricing.PricingStrategyFactory;
import com.moviebooking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds realistic demo data on first startup so the live demo is never empty.
 *
 * <p>Full seed guard: runs only when {@code movieRepository.count() == 0}.
 * Show re-seed: if movies exist but all shows are in the past (deploy was weeks ago),
 * old shows/bookings are wiped and fresh shows are created relative to now.
 */
@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PricingStrategyFactory pricingFactory;

    public DataInitializer(MovieRepository movieRepository,
                           TheaterRepository theaterRepository,
                           ScreenRepository screenRepository,
                           SeatRepository seatRepository,
                           ShowRepository showRepository,
                           ShowSeatRepository showSeatRepository,
                           BookingRepository bookingRepository,
                           UserRepository userRepository,
                           PricingStrategyFactory pricingFactory) {
        this.movieRepository = movieRepository;
        this.theaterRepository = theaterRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.pricingFactory = pricingFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (movieRepository.count() == 0) {
            log.info("DataInitializer: fresh DB — seeding all demo data...");
            seedAll();
            return;
        }

        // Movies exist but shows may have aged out (seeded once, dates relative to first boot).
        // Re-seed shows so the demo always has bookable content.
        if (showRepository.count() > 0 &&
                showRepository.countByStartTimeAfter(LocalDateTime.now()) == 0) {
            log.info("DataInitializer: all shows are in the past — re-seeding shows...");
            reseedShows();
        } else {
            log.info("DataInitializer: data present and shows are current, skipping seed.");
        }
    }

    private void seedAll() {
        // ── Users ──────────────────────────────────────────────────────────────
        userRepository.saveAll(List.of(
                new User("Alice Johnson", "alice@example.com", "+1-555-0101"),
                new User("Bob Smith", "bob@example.com", "+1-555-0202"),
                new User("Carol White", "carol@example.com", "+1-555-0303")
        ));

        // ── Movies ─────────────────────────────────────────────────────────────
        List<Movie> movies = movieRepository.saveAll(List.of(
                new Movie("Inception", "SCI-FI", "A mind-bending thriller about dream heists.",
                        148, "English", LocalDate.of(2010, 7, 16),
                        "https://image.tmdb.org/t/p/w500/inception.jpg", 8.8),
                new Movie("The Grand Budapest Hotel", "COMEDY",
                        "A whimsical adventure across a fading European republic.",
                        99, "English", LocalDate.of(2014, 3, 28),
                        "https://image.tmdb.org/t/p/w500/gbh.jpg", 8.1),
                new Movie("Parasite", "THRILLER",
                        "A dark comedy thriller about class inequality.",
                        132, "Korean", LocalDate.of(2019, 5, 30),
                        "https://image.tmdb.org/t/p/w500/parasite.jpg", 8.5),
                new Movie("Everything Everywhere All at Once", "SCI-FI",
                        "A multiverse adventure full of heart and absurdity.",
                        139, "English", LocalDate.of(2022, 3, 25),
                        "https://image.tmdb.org/t/p/w500/eeaao.jpg", 7.8),
                new Movie("The Secret Life of Walter Mitty", "DRAMA",
                        "A daydreamer embarks on a real-life adventure.",
                        114, "English", LocalDate.of(2013, 12, 25),
                        "https://image.tmdb.org/t/p/w500/waltermitty.jpg", 7.3)
        ));

        // ── Theaters ───────────────────────────────────────────────────────────
        Theater cineplex = theaterRepository.save(
                new Theater("Cineplex IMAX", "San Francisco", "100 Market St, SF, CA 94103"));
        Theater starlight = theaterRepository.save(
                new Theater("Starlight Cinema", "San Francisco", "456 Castro St, SF, CA 94114"));

        // ── Screens ───────────────────────────────────────────────────────────
        Screen screen1 = screenRepository.save(new Screen(cineplex, "IMAX Screen 1", 60));
        Screen screen2 = screenRepository.save(new Screen(cineplex, "Screen 2", 40));
        Screen screen3 = screenRepository.save(new Screen(starlight, "Screen A", 50));

        // ── Seats for each screen ─────────────────────────────────────────────
        createSeats(screen1, 6, 10);   // 6 rows × 10 cols = 60 seats
        createSeats(screen2, 5, 8);    // 5 rows × 8 cols  = 40 seats
        createSeats(screen3, 5, 10);   // 5 rows × 10 cols = 50 seats

        List<Show> shows = createShows(movies);
        createShowSeats(shows);

        log.info("DataInitializer: seeded {} movies, 2 theaters, 3 screens, {} shows.",
                movies.size(), shows.size());
    }

    /**
     * Wipes all bookings, show-seats, and shows, then recreates shows relative to now.
     * Movies, theaters, screens, and seats are preserved.
     */
    private void reseedShows() {
        // Delete in FK-safe order: bookings (cascades to booking_seats) → show_seats → shows
        bookingRepository.deleteAll();
        showSeatRepository.deleteAll();
        showRepository.deleteAll();

        List<Movie> movies = movieRepository.findAll();
        List<Show> shows = createShows(movies);
        createShowSeats(shows);

        log.info("DataInitializer: re-seeded {} shows with fresh dates.", shows.size());
    }

    private List<Show> createShows(List<Movie> movies) {
        List<Screen> screens = screenRepository.findAll();
        Screen screen1 = screens.get(0);
        Screen screen2 = screens.get(1);
        Screen screen3 = screens.get(2);

        LocalDateTime base = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        return showRepository.saveAll(List.of(
                new Show(movies.get(0), screen1, base.plusDays(1),
                        base.plusDays(1).plusMinutes(148), new BigDecimal("15.00"), "English"),
                new Show(movies.get(0), screen1, base.plusDays(1).plusHours(4),
                        base.plusDays(1).plusHours(4).plusMinutes(148), new BigDecimal("15.00"), "English"),
                new Show(movies.get(1), screen2, base.plusDays(1),
                        base.plusDays(1).plusMinutes(99), new BigDecimal("12.00"), "English"),
                new Show(movies.get(2), screen3, base.plusDays(2),
                        base.plusDays(2).plusMinutes(132), new BigDecimal("13.00"), "Korean"),
                new Show(movies.get(3), screen1, base.plusDays(2),
                        base.plusDays(2).plusMinutes(139), new BigDecimal("14.00"), "English"),
                new Show(movies.get(4), screen2, base.plusDays(3),
                        base.plusDays(3).plusMinutes(114), new BigDecimal("12.00"), "English"),
                // Weekend shows (Fri–Sun) for pricing strategy demo
                new Show(movies.get(0), screen3, base.plusDays(5),
                        base.plusDays(5).plusMinutes(148), new BigDecimal("15.00"), "English"),
                new Show(movies.get(2), screen1, base.plusDays(6),
                        base.plusDays(6).plusMinutes(132), new BigDecimal("13.00"), "Korean")
        ));
    }

    private void createShowSeats(List<Show> shows) {
        for (Show show : shows) {
            List<Seat> seats = seatRepository.findByScreenId(show.getScreen().getId());
            List<ShowSeat> showSeats = seats.stream()
                    .map(seat -> {
                        BigDecimal price = pricingFactory
                                .getStrategy(show, seat)
                                .calculatePrice(show.getBasePrice(), show, seat);
                        return new ShowSeat(show, seat, price);
                    })
                    .toList();
            showSeatRepository.saveAll(showSeats);
        }
    }

    /** Creates rows A–(rowLabel) × columns 1–cols, last 2 rows are PREMIUM. */
    private void createSeats(Screen screen, int rows, int cols) {
        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String rowLabel = String.valueOf((char) ('A' + r));
            SeatType type = (r >= rows - 2) ? SeatType.PREMIUM : SeatType.REGULAR;
            for (int c = 1; c <= cols; c++) {
                seats.add(new Seat(screen, rowLabel, c, type));
            }
        }
        seatRepository.saveAll(seats);
    }
}
