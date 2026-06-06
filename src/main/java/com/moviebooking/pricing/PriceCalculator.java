package com.moviebooking.pricing;

import java.math.BigDecimal;

/**
 * Custom functional interface — demonstrates Java 8+ FunctionalInterface concept.
 *
 * <p>Encapsulates the arithmetic of applying a multiplier to a base price.
 * Used inside PricingStrategy implementations via lambda / method reference.
 *
 * <pre>{@code
 * PriceCalculator calculator = (base, multiplier) ->
 *     base.multiply(BigDecimal.valueOf(multiplier))
 *         .setScale(2, RoundingMode.HALF_UP);
 * }</pre>
 */
@FunctionalInterface
public interface PriceCalculator {

    /**
     * Apply a multiplier to a base price.
     *
     * @param basePrice  the show's raw base price
     * @param multiplier e.g. 1.0 (weekday regular), 1.25 (weekend), 1.5 (premium)
     * @return final calculated price
     */
    BigDecimal calculate(BigDecimal basePrice, double multiplier);
}
