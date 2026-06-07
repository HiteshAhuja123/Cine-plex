package com.moviebooking.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Agentic AI booking assistant — Spring AI 1.0.0 implementation.
 *
 * <h2>How the agentic loop works</h2>
 * <p>1. User sends a natural-language request ("book 2 sci-fi seats this weekend").<br>
 * 2. Spring AI sends the message + tool schemas (from {@link BookingTools} {@code @Tool} methods) to Gemini.<br>
 * 3. Gemini decides which tools to call and in what order.<br>
 * 4. Spring AI executes each tool against the real service layer and feeds results back.<br>
 * 5. Gemini continues until it produces a final text answer.<br>
 *
 * <h2>Provider</h2>
 * <p>Uses {@code spring-ai-starter-model-openai} pointed at Google's OpenAI-compatible
 * Gemini endpoint ({@code generativelanguage.googleapis.com/v1beta/openai}).
 * Same {@code GEMINI_API_KEY}, same models, function-calling fully supported.
 *
 * <h2>Graceful degradation</h2>
 * <p>If {@code GEMINI_API_KEY} is absent, {@code chatClient} is null and the service
 * returns a clear message — the rest of the API still works normally.
 */
@Service
public class BookingAssistantService {

    private static final Logger log = LoggerFactory.getLogger(BookingAssistantService.class);

    private static final String SYSTEM_PROMPT = """
            You are a helpful movie ticket booking assistant.
            Your job is to help users find movies, check show times, and book tickets.

            RULES:
            1. ONLY suggest movies and shows that actually exist in the database — use searchMovies and listShows tools first.
            2. ALWAYS follow this order: searchMovies → listShows → checkSeatAvailability → holdSeats → confirmBooking.
            3. Do not invent movie titles, show times, or seat IDs. Only use IDs returned by tools.
            4. If the user asks to "book" something, complete the full flow through confirmBooking unless they say "hold only".
            5. Summarise the final booking: movie, time, theater, seats, total amount.
            6. CRITICAL: invoke tools using the structured JSON tool-call mechanism only. Never write tool calls as \
            <function=name{...}> or any XML/text format — that format is invalid and will cause an error.
            """;

    private final ChatClient chatClient;
    private final BookingTools bookingTools;

    // @Autowired on the constructor tells Spring which one to use (two constructors exist).
    // @Nullable on the builder lets Spring pass null when GEMINI_API_KEY is absent.
    @Autowired
    public BookingAssistantService(@Nullable ChatClient.Builder chatClientBuilder,
                                   BookingTools bookingTools) {
        this.chatClient = chatClientBuilder != null ? chatClientBuilder.build() : null;
        this.bookingTools = bookingTools;
    }

    // Package-private: allows unit tests to inject a pre-built mock ChatClient directly.
    BookingAssistantService(ChatClient chatClient, BookingTools bookingTools) {
        this.chatClient = chatClient;
        this.bookingTools = bookingTools;
    }

    /**
     * Runs the user's natural-language request through the agentic tool loop.
     *
     * @param userId  scopes booking actions — injected into the enriched message
     * @param message free-text user request
     */
    public AssistantResponse chat(Long userId, String message) {
        if (chatClient == null) {
            return new AssistantResponse(
                    "AI assistant is unavailable: GEMINI_API_KEY is not configured. "
                    + "All booking endpoints work normally via REST.",
                    false, "unavailable");
        }

        try {
            log.debug("[Assistant] userId={} message={}", userId, message);

            // Embed userId so the LLM passes the correct value to holdSeats/confirmBooking.
            String enrichedMessage = message + "  [Context: the current user's id is " + userId + "]";

            String reply = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(enrichedMessage)
                    .toolCallbacks(MethodToolCallbackProvider.builder()
                            .toolObjects(bookingTools)
                            .build())
                    .call()
                    .content();

            return new AssistantResponse(reply, true, "gemini-2.0-flash");

        } catch (Exception e) {
            log.error("[Assistant] Error: {}", e.getMessage(), e);
            return new AssistantResponse(
                    "Sorry, the assistant encountered an error: " + e.getMessage(),
                    false, "gemini-2.0-flash");
        }
    }
}
