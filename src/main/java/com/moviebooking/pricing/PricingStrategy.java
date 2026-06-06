package com.moviebooking.pricing;

import com.moviebooking.entity.Seat;
import com.moviebooking.entity.Show;

import java.math.BigDecimal;

/**
 * Strategy interface for computing the price of one seat at one show.
 *
 * <p>Demonstrates the <b>Strategy pattern</b>: the algorithm (price multiplier logic)
 * is encapsulated behind this interface, and the concrete implementation is chosen
 * at runtime by {@link PricingStrategyFactory} based on show date and seat type.
 *
 * <p>Polymorphism: every concrete strategy is substitutable for any other,
 * and BookingService calls {@code calculatePrice()} without knowing which
 * implementation it's working with.
 */
public interface PricingStrategy {

    /**
     * Calculate the final price for a single seat.
     *
     * @param basePrice the show's base ticket price
     * @param show      the screening (used to check day-of-week)
     * @param seat      the specific seat (used to check SeatType)
     * @return final price to store on ShowSeat
     */
    BigDecimal calculatePrice(BigDecimal basePrice, Show show, Seat seat);

    /** Human-readable strategy name — useful for logging and debugging. */
    String name();
}
