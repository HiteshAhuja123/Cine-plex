package com.moviebooking.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.tool.ToolCallbackProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingAssistantService")
class BookingAssistantServiceTest {

    @Mock private BookingTools bookingTools;

    @Test
    @DisplayName("returns unavailable message when GEMINI_API_KEY is absent (chatClientBuilder = null)")
    void chat_returnsUnavailableMessage_whenChatClientBuilderIsNull() {
        var service = new BookingAssistantService((ChatClient) null, bookingTools);

        AssistantResponse response = service.chat(1L, "book 2 seats for Interstellar");

        assertThat(response.reply()).contains("unavailable");
        assertThat(response.toolsUsed()).isFalse();
        assertThat(response.model()).isEqualTo("unavailable");
    }

    @Test
    @DisplayName("returns error message when the LLM call throws an exception")
    void chat_returnsErrorMessage_whenLlmThrows() {
        ChatClient mockClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.prompt()
                .system(anyString())
                .user(anyString())
                .toolCallbacks(any(ToolCallbackProvider.class))
                .call()
                .content()
        ).thenThrow(new RuntimeException("API timeout"));

        var service = new BookingAssistantService(mockClient, bookingTools);

        AssistantResponse response = service.chat(1L, "book 2 seats");

        assertThat(response.reply()).contains("error");
        assertThat(response.toolsUsed()).isFalse();
    }

    @Test
    @DisplayName("returns LLM reply with toolsUsed=true on successful call")
    void chat_returnsReply_onSuccess() {
        ChatClient mockClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(mockClient.prompt()
                .system(anyString())
                .user(anyString())
                .toolCallbacks(any(ToolCallbackProvider.class))
                .call()
                .content()
        ).thenReturn("Booking confirmed for Interstellar — seats A1, A2. Total: ₹500.");

        var service = new BookingAssistantService(mockClient, bookingTools);

        AssistantResponse response = service.chat(1L, "book 2 seats for Interstellar");

        assertThat(response.reply()).contains("Interstellar").contains("A1");
        assertThat(response.toolsUsed()).isTrue();
        assertThat(response.model()).isEqualTo("gemini-2.0-flash");
    }
}
