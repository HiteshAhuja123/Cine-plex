package com.moviebooking.pricing;

import com.moviebooking.entity.Seat;
import com.moviebooking.entity.Show;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decorator-style strategy for PREMIUM seats.
 *
 * <p>Wraps another {@link PricingStrategy} (weekday or weekend) and adds a
 * 50% premium-seat surcharge on top of whatever the base strategy returns.
 * This demonstrates <b>composition over inheritance</b> and the Decorator pattern.
 *
 * <p>It is NOT a Spring @Component because it is always instantiated by
 * {@link PricingStrategyFactory} with a specific inner strategy injected.
 */
public class PremiumSeatPricingStrategy implements PricingStrategy {

    private final PricingStrategy baseStrategy;

    private final PriceCalculator premiumCalculator =
            (base, multiplier) -> base.multiply(BigDecimal.valueOf(multiplier))
                                       .setScale(2, RoundingMode.HALF_UP);

    public PremiumSeatPricingStrategy(PricingStrategy baseStrategy) {
        this.baseStrategy = baseStrategy;
    }

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice, Show show, Seat seat) {
        BigDecimal dayAdjustedPrice = baseStrategy.calculatePrice(basePrice, show, seat);
        // Apply 50% premium surcharge on top of the day-adjusted base
        return premiumCalculator.calculate(dayAdjustedPrice, 1.50);
    }

    @Override
    public String name() {
        return "PREMIUM_" + baseStrategy.name();
    }
}
