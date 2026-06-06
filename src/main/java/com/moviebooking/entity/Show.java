package com.moviebooking.entity;

import com.moviebooking.entity.enums.ShowStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single screening event: one Movie on one Screen at a specific time.
 *
 * <p>{@code basePrice} is the anchor used by PricingStrategy; the final per-seat
 * price (stored on ShowSeat) is computed at hold time.
 *
 * <p>Index on {@code start_time} is critical for "upcoming shows" queries.
 */
@Entity
@Table(name = "shows",
       indexes = {
           @Index(name = "idx_shows_start_time", columnList = "start_time"),
           @Index(name = "idx_shows_movie_id", columnList = "movie_id"),
           @Index(name = "idx_shows_screen_id", columnList = "screen_id")
       })
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowStatus status = ShowStatus.ACTIVE;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShowSeat> showSeats = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────────────────

    public Show() {}

    public Show(Movie movie, Screen screen, LocalDateTime startTime,
                LocalDateTime endTime, BigDecimal basePrice, String language) {
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.language = language;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public ShowStatus getStatus() { return status; }
    public void setStatus(ShowStatus status) { this.status = status; }
    public List<ShowSeat> getShowSeats() { return showSeats; }
}
