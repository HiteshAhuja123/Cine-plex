package com.moviebooking.ai;

import com.moviebooking.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Agentic AI assistant endpoint.
 *
 * <p>POST /api/assistant with a natural-language request; the assistant plans and
 * executes the booking tool chain autonomously.
 *
 * <p>Example request body:
 * <pre>{@code
 * {
 *   "userId": 1,
 *   "message": "Book 2 regular seats for a sci-fi movie this weekend"
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/assistant")
@Tag(name = "AI Assistant",
     description = "Agentic AI that plans and executes booking tool calls from natural language")
public class BookingAssistantController {

    private final BookingAssistantService assistantService;

    public BookingAssistantController(BookingAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Send a natural-language booking request to the AI assistant.
     *
     * <p>The assistant will call tools (searchMovies, listShows, checkSeatAvailability,
     * holdSeats, confirmBooking) in sequence as needed. Gracefully degrades to an
     * informative message if GEMINI_API_KEY is not set.
     *
     * @param request contains userId and natural-language message
     */
    @PostMapping
    @Operation(
            summary = "AI booking assistant",
            description = """
                    Send a natural-language request. The Gemini-powered assistant will:
                    1. Call searchMovies to find matching titles
                    2. Call listShows to find available screenings
                    3. Call checkSeatAvailability to get seat IDs
                    4. Call holdSeats (through the same concurrency-safe BookingService)
                    5. Call confirmBooking to finalise

                    Requires GEMINI_API_KEY env var. Degrades gracefully if absent.
                    """
    )
    public ResponseEntity<ApiResponse<AssistantResponse>> chat(
            @Valid @RequestBody AssistantRequest request) {
        AssistantResponse response = assistantService.chat(request.userId(), request.message());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
