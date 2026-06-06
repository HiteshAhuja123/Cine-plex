package com.moviebooking.ai;

/**
 * No longer needed — tool registration is handled by {@link BookingTools} via
 * {@code @Tool} annotations + {@code MethodToolCallbackProvider} in
 * {@link BookingAssistantService}.
 *
 * <p>In Spring AI 1.0.0 the old {@code @Bean Function<I,O>} approach was replaced
 * by method-level {@code @Tool} annotations. This file is kept as a marker so the
 * git history shows the migration path.
 */
public final class BookingToolsConfig {
    private BookingToolsConfig() {}
}
