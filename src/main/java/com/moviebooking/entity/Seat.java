package com.moviebooking.entity;

import com.moviebooking.entity.enums.SeatType;
import jakarta.persistence.*;

/**
 * A physical seat inside a Screen.
 *
 * <p>rowLabel ("A"–"Z") × columnNumber (1–N) produces a human-readable seatCode like "C7".
 * SeatType drives the PricingStrategy multiplier at booking time.
 */
@Entity
@Table(name = "seats",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_seats_screen_row_col",
           columnNames = {"screen_id", "row_label", "column_number"}),
       indexes = @Index(name = "idx_seats_screen", columnList = "screen_id"))
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "row_label", nullable = false, length = 3)
    private String rowLabel;      // "A", "B", "C" …

    @Column(name = "column_number", nullable = false)
    private Integer columnNumber; // 1, 2, 3 …

    @Column(name = "seat_code", nullable = false, length = 8)
    private String seatCode;      // "A1", "B12" — derived, stored for query convenience

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Seat() {}

    public Seat(Screen screen, String rowLabel, Integer columnNumber, SeatType seatType) {
        this.screen = screen;
        this.rowLabel = rowLabel;
        this.columnNumber = columnNumber;
        this.seatCode = rowLabel + columnNumber;
        this.seatType = seatType;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }
    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
    public Integer getColumnNumber() { return columnNumber; }
    public void setColumnNumber(Integer columnNumber) { this.columnNumber = columnNumber; }
    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }
    public SeatType getSeatType() { return seatType; }
    public void setSeatType(SeatType seatType) { this.seatType = seatType; }
}
