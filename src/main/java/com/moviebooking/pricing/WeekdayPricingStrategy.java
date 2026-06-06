package com.moviebooking.pricing;

import com.moviebooking.entity.Seat;
import com.moviebooking.entity.Show;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Weekday (Mon–Thu) pricing for REGULAR seats — no markup, base price only.
 *
 * <p>Uses a {@link PriceCalculator} lambda to demonstrate FunctionalInterface usage
 * with a method reference as the arithmetic implementation.
 */
@Component
public class WeekdayPricingStrategy implements PricingStrategy {

    /** Lambda assigned to the FunctionalInterface — multiply and round. */
    private final PriceCalculator calculator =
            (base, multiplier) -> base.multiply(BigDecimal.valueOf(multiplier))
                                       .setScale(2, RoundingMode.HALF_UP);

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice, Show show, Seat seat) {
        return calculator.calculate(basePrice, 1.0);
    }

    @Override
    public String name() {
        return "WEEKDAY_REGULAR";
    }
}
