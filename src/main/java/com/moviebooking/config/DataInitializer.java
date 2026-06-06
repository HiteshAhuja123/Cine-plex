package com.moviebooking.config;

import com.moviebooking.entity.*;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.pricing.PricingStrategyFactory;
import com.moviebooking.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.Map;

/**
 * Seeds demo data on first startup so the live demo is never empty.
 *
 * <p>Strategy:
 * <ul>
 *   <li>If the current movie lineup (keyed on "The Dark Knight") is absent → purge everything
 *       and re-seed. Handles both fresh DBs and old deployments with a stale lineup.</li>
 *   <li>If the lineup is current but all shows are in the past → re-seed shows only.</li>
 *   <li>Always patches poster URLs in case they were saved incorrectly.</li>
 * </ul>
 */
@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    // Verified TMDB poster hashes fetched directly from themoviedb.org
    private static final Map<String, String> POSTER_URLS = Map.of(
            "Inception",
            "https://image.tmdb.org/t/p/w500/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg",
            "The Dark Knight",
            "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            "Interstellar",
            "https://image.tmdb.org/t/p/w500/yQvGrMoipbRoddT0ZR8tPoR7NfX.jpg",
            "Oppenheimer",
            "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            "Dune: Part Two",
            "https://image.tmdb.org/t/p/w500/3HzGtM0JpfH2pWFGugJK22LRP6b.jpg",
            "Parasite",
            "https://image.tmdb.org/t/p/w500/igICOruFgiqdY1HXwTNRuXJute.jpg",
            "Spider-Man: No Way Home",
            "https://image.tmdb.org/t/p/w500/tJ44EffQBBUMc61xa8QDz0oijQT.jpg",
            "Top Gun: Maverick",
            "https://image.tmdb.org/t/p/w500/n0YuM4f5lvGAP6MAW2kBIzugXnc.jpg"
    );

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
        // "The Dark Knight" marks the current lineup — absent means fresh DB or stale data.
        if (!movieRepository.existsByTitle("The Dark Knight")) {
            log.info("DataInitializer: current lineup absent — purging and re-seeding...");
            purgeAll();
            seedAll();
        } else if (showRepository.count() > 0 &&
                showRepository.countByStartTimeAfter(LocalDateTime.now()) == 0) {
            log.info("DataInitializer: shows expired — re-seeding shows...");
            reseedShows();
        } else {
            log.info("DataInitializer: data current, skipping seed.");
        }

        fixPosterUrls();
    }

    // ── Seed helpers ──────────────────────────────────────────────────────────

    private void seedAll() {
        userRepository.saveAll(List.of(
                new User("Alice Johnson", "alice@example.com", "+1-555-0101"),
                new User("Bob Smith", "bob@example.com", "+1-555-0202"),
                new User("Carol White", "carol@example.com", "+1-555-0303")
        ));

        List<Movie> movies = movieRepository.saveAll(List.of(
                new Movie("Inception", "SCI-FI",
                        "A thief who steals corporate secrets through dream-sharing technology.",
                        148, "English", LocalDate.of(2010, 7, 16),
                        POSTER_URLS.get("Inception"), 8.4),
                new Movie("The Dark Knight", "ACTION",
                        "Batman faces the Joker, a criminal mastermind who plunges Gotham into chaos.",
                        152, "English", LocalDate.of(2008, 7, 18),
                        POSTER_URLS.get("The Dark Knight"), 9.0),
                new Movie("Interstellar", "SCI-FI",
                        "A team of explorers travel through a wormhole in space to ensure humanity's survival.",
                        169, "English", LocalDate.of(2014, 11, 7),
                        POSTER_URLS.get("Interstellar"), 8.6),
                new Movie("Oppenheimer", "DRAMA",
                        "The story of J. Robert Oppenheimer and the development of the atomic bomb.",
                        180, "English", LocalDate.of(2023, 7, 21),
                        POSTER_URLS.get("Oppenheimer"), 8.1),
                new Movie("Dune: Part Two", "SCI-FI",
                        "Paul Atreides unites with the Fremen to wage war against House Harkonnen.",
                        166, "English", LocalDate.of(2024, 3, 1),
                        POSTER_URLS.get("Dune: Part Two"), 8.2),
                new Movie("Parasite", "THRILLER",
                        "A poor family schemes to become employed by a wealthy household.",
                        132, "Korean", LocalDate.of(2019, 5, 30),
                        POSTER_URLS.get("Parasite"), 8.5),
                new Movie("Spider-Man: No Way Home", "ACTION",
                        "Spider-Man asks Doctor Strange to make the world forget his identity.",
                        148, "English", LocalDate.of(2021, 12, 17),
                        POSTER_URLS.get("Spider-Man: No Way Home"), 7.9),
                new Movie("Top Gun: Maverick", "ACTION",
                        "After thirty years, Maverick is called to train a new generation of Top Gun graduates.",
                        130, "English", LocalDate.of(2022, 5, 27),
                        POSTER_URLS.get("Top Gun: Maverick"), 8.3)
        ));

        Theater cineplex = theaterRepository.save(
                new Theater("Cineplex IMAX", "San Francisco", "100 Market St, SF, CA 94103"));
        Theater starlight = theaterRepository.save(
                new Theater("Starlight Cinema", "San Francisco", "456 Castro St, SF, CA 94114"));

        Screen screen1 = screenRepository.save(new Screen(cineplex, "IMAX Screen 1", 60));
        Screen screen2 = screenRepository.save(new Screen(cineplex, "Screen 2", 40));
        Screen screen3 = screenRepository.save(new Screen(starlight, "Screen A", 50));

        createSeats(screen1, 6, 10);
        createSeats(screen2, 5, 8);
        createSeats(screen3, 5, 10);

        List<Show> shows = createShows(movies);
        createShowSeats(shows);

        log.info("DataInitializer: seeded {} movies, 2 theaters, 3 screens, {} shows.",
                movies.size(), shows.size());
    }

    private void reseedShows() {
        bookingRepository.deleteAll();
        showSeatRepository.deleteAll();
        showRepository.deleteAll();

        List<Movie> movies = movieRepository.findAll();
        List<Show> shows = createShows(movies);
        createShowSeats(shows);

        log.info("DataInitializer: re-seeded {} shows with fresh dates.", shows.size());
    }

    /** Wipes all data in FK-safe order before a full re-seed. */
    private void purgeAll() {
        // Native SQL DELETEs execute immediately (bypass JPA buffering) in FK-safe order.
        // JPA's deleteAll() queues deletes and can cause constraint violations when inserts
        // follow in the same transaction — native queries avoid that entirely.
        entityManager.createNativeQuery("DELETE FROM booking_seats").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM bookings").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM show_seats").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM shows").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM seats").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM screens").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM theaters").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM movies").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
        entityManager.clear(); // clear first-level cache so subsequent inserts start fresh
    }

    private List<Show> createShows(List<Movie> movies) {
        List<Screen> screens = screenRepository.findAll();
        Screen screen1 = screens.get(0);
        Screen screen2 = screens.get(1);
        Screen screen3 = screens.get(2);

        // Helper to find movie by title
        java.util.function.Function<String, Movie> byTitle = title -> movies.stream()
                .filter(m -> m.getTitle().equals(title))
                .findFirst()
                .orElse(movies.get(0));

        LocalDateTime base = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        return showRepository.saveAll(List.of(
                // IMAX Screen 1 — blockbusters
                new Show(byTitle.apply("Inception"), screen1,
                        base.plusDays(1), base.plusDays(1).plusMinutes(148),
                        new BigDecimal("18.00"), "English"),
                new Show(byTitle.apply("The Dark Knight"), screen1,
                        base.plusDays(1).plusHours(4), base.plusDays(1).plusHours(4).plusMinutes(152),
                        new BigDecimal("18.00"), "English"),
                new Show(byTitle.apply("Oppenheimer"), screen1,
                        base.plusDays(2), base.plusDays(2).plusMinutes(180),
                        new BigDecimal("18.00"), "English"),
                // Screen 2 — mix
                new Show(byTitle.apply("Interstellar"), screen2,
                        base.plusDays(1), base.plusDays(1).plusMinutes(169),
                        new BigDecimal("14.00"), "English"),
                new Show(byTitle.apply("Spider-Man: No Way Home"), screen2,
                        base.plusDays(2), base.plusDays(2).plusMinutes(148),
                        new BigDecimal("14.00"), "English"),
                new Show(byTitle.apply("Top Gun: Maverick"), screen2,
                        base.plusDays(3), base.plusDays(3).plusMinutes(130),
                        new BigDecimal("14.00"), "English"),
                // Screen A — Starlight
                new Show(byTitle.apply("Dune: Part Two"), screen3,
                        base.plusDays(1), base.plusDays(1).plusMinutes(166),
                        new BigDecimal("15.00"), "English"),
                new Show(byTitle.apply("Parasite"), screen3,
                        base.plusDays(2), base.plusDays(2).plusMinutes(132),
                        new BigDecimal("13.00"), "Korean"),
                // Weekend shows for pricing strategy demo
                new Show(byTitle.apply("The Dark Knight"), screen1,
                        base.plusDays(5), base.plusDays(5).plusMinutes(152),
                        new BigDecimal("18.00"), "English"),
                new Show(byTitle.apply("Dune: Part Two"), screen3,
                        base.plusDays(6), base.plusDays(6).plusMinutes(166),
                        new BigDecimal("15.00"), "English")
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

    /** Patches any poster URL that doesn't match the verified TMDB hash. Runs every startup. */
    private void fixPosterUrls() {
        movieRepository.findAll().forEach(movie -> {
            String correct = POSTER_URLS.get(movie.getTitle());
            if (correct != null && !correct.equals(movie.getPosterUrl())) {
                movie.setPosterUrl(correct);
                movieRepository.save(movie);
                log.info("DataInitializer: patched poster URL for '{}'", movie.getTitle());
            }
        });
    }

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
