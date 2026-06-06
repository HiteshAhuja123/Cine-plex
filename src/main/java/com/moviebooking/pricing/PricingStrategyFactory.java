package com.moviebooking.pricing;

import com.moviebooking.entity.Seat;
import com.moviebooking.entity.Show;
import com.moviebooking.entity.enums.SeatType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

/**
 * Selects the correct {@link PricingStrategy} at runtime based on the show's
 * day-of-week and the seat's tier — demonstrating runtime polymorphism.
 *
 * <p>Decision table:
 * <pre>
 * Day         | SeatType | Strategy
 * Mon–Thu     | REGULAR  | WeekdayPricingStrategy      (×1.0)
 * Fri–Sun     | REGULAR  | WeekendPricingStrategy      (×1.25)
 * Mon–Thu     | PREMIUM  | PremiumSeatPricingStrategy wrapping Weekday   (×1.5)
 * Fri–Sun     | PREMIUM  | PremiumSeatPricingStrategy wrapping Weekend   (×1.875)
 * </pre>
 *
 * <p>Uses constructor injection — no field injection, for testability.
 */
@Component
public class PricingStrategyFactory {

    private final WeekdayPricingStrategy weekdayStrategy;
    private final WeekendPricingStrategy weekendStrategy;

    public PricingStrategyFactory(WeekdayPricingStrategy weekdayStrategy,
                                  WeekendPricingStrategy weekendStrategy) {
        this.weekdayStrategy = weekdayStrategy;
        this.weekendStrategy = weekendStrategy;
    }

    /**
     * Returns the appropriate strategy for the given show and seat combination.
     * This is the runtime polymorphism point — callers work against {@link PricingStrategy}.
     */
    public PricingStrategy getStrategy(Show show, Seat seat) {
        boolean isWeekend = isWeekend(show.getStartTime().getDayOfWeek());

        // Switch expression (Java 14+) — cleaner than if/else chains
        PricingStrategy dayStrategy = switch (isWeekend ? "WEEKEND" : "WEEKDAY") {
            case "WEEKEND" -> weekendStrategy;
            default -> weekdayStrategy;
        };

        return seat.getSeatType() == SeatType.PREMIUM
                ? new PremiumSeatPricingStrategy(dayStrategy)
                : dayStrategy;
    }

    private boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.FRIDAY
                || day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }
}
