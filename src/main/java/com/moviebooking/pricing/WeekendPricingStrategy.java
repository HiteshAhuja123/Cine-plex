package com.moviebooking.pricing;

import com.moviebooking.entity.Seat;
import com.moviebooking.entity.Show;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Friday–Sunday pricing for REGULAR seats — 25% weekend surcharge. */
@Component
public class WeekendPricingStrategy implements PricingStrategy {

    private final PriceCalculator calculator =
            (base, multiplier) -> base.multiply(BigDecimal.valueOf(multiplier))
                                       .setScale(2, RoundingMode.HALF_UP);

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice, Show show, Seat seat) {
        return calculator.calculate(basePrice, 1.25);
    }

    @Override
    public String name() {
        return "WEEKEND_REGULAR";
    }
}
