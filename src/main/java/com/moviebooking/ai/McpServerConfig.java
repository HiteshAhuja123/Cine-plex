package com.moviebooking.ai;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the booking tools as an MCP (Model Context Protocol) server tool provider.
 *
 * <p>Spring AI's MCP server autoconfiguration discovers every {@link ToolCallbackProvider}
 * bean in the context and exposes them as MCP tools over SSE at {@code /mcp/sse}.
 *
 * <p>All tool calls still go through {@link BookingTools} → {@link com.moviebooking.service.BookingService},
 * so the pessimistic {@code SELECT FOR UPDATE} lock and {@code @Transactional} guarantees
 * from Priority 1 are fully preserved for external MCP clients too.
 *
 * <h2>Connecting a client</h2>
 * <pre>
 * Claude Desktop config (~/.claude/claude_desktop_config.json):
 * {
 *   "mcpServers": {
 *     "movie-booking": {
 *       "url": "http://localhost:8080/mcp/sse"
 *     }
 *   }
 * }
 * </pre>
 */
@Configuration
public class McpServerConfig {

    /**
     * Wraps all {@code @Tool}-annotated methods in {@link BookingTools} into a
     * {@link ToolCallbackProvider} that the MCP server autoconfiguration picks up.
     *
     * <p>This bean is separate from the inline {@code MethodToolCallbackProvider} built
     * inside {@link BookingAssistantService#chat} — one is for the ChatClient tool-calling
     * loop, this one is for the MCP server's tool registry.
     */
    @Bean
    public ToolCallbackProvider bookingMcpTools(BookingTools bookingTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(bookingTools)
                .build();
    }
}
