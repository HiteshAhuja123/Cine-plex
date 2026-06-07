# CONCEPTS.md — Concept-to-Code Map

This table maps every Java, Spring, DBMS, concurrency, and AI concept demonstrated
in this project to the exact file(s) where a reviewer can verify it.

---

## Java Core Concepts

| Concept | Where to find it | Notes |
|---------|-----------------|-------|
| **Generics** | [`ApiResponse.java`](src/main/java/com/moviebooking/dto/ApiResponse.java) | `ApiResponse<T>` — unconstrained generic, used by all 20+ endpoints |
| **Records (Java 16+)** | Every file in [`dto/request/`](src/main/java/com/moviebooking/dto/request/) and [`dto/response/`](src/main/java/com/moviebooking/dto/response/) | Immutable, compact DTOs with zero boilerplate |
| **Custom Functional Interface** | [`PriceCalculator.java`](src/main/java/com/moviebooking/pricing/PriceCalculator.java) | `@FunctionalInterface` — used via lambda in each pricing strategy |
| **Lambda expressions** | [`WeekdayPricingStrategy.java`](src/main/java/com/moviebooking/pricing/WeekdayPricingStrategy.java), [`WeekendPricingStrategy.java`](src/main/java/com/moviebooking/pricing/WeekendPricingStrategy.java) | `(base, multiplier) -> base.multiply(...)` assigned to `PriceCalculator` |
| **Method references** | [`MovieMapper.java`](src/main/java/com/moviebooking/mapper/MovieMapper.java) line ~28 | `movies.stream().map(MovieMapper::toResponse)` |
| **Streams + lambdas (aggregation)** | [`BookingService.java`](src/main/java/com/moviebooking/service/BookingService.java), [`MovieMapper.java`](src/main/java/com/moviebooking/mapper/MovieMapper.java) | Filter, map, collect on entity/DTO collections |
| **Optional** | [`MovieService.java`](src/main/java/com/moviebooking/service/MovieService.java), all service layer `findById().orElseThrow(...)` | Safe null handling without null checks |
| **Switch expressions (Java 14+)** | [`BookingService.java`](src/main/java/com/moviebooking/service/BookingService.java) `confirmBooking()` | `return switch (booking.getStatus()) { case HELD -> ...; ... }` |
| **Switch expressions** | [`PricingStrategyFactory.java`](src/main/java/com/moviebooking/pricing/PricingStrategyFactory.java) `getStrategy()` | Weekday vs. weekend selection |
| **java.time** | [`Show.java`](src/main/java/com/moviebooking/entity/Show.java), [`Booking.java`](src/main/java/com/moviebooking/entity/Booking.java), [`SeatHoldExpiryScheduler.java`](src/main/java/com/moviebooking/scheduler/SeatHoldExpiryScheduler.java) | `LocalDateTime`, `LocalDate`, `DayOfWeek` throughout |
| **Enums** | [`SeatType.java`](src/main/java/com/moviebooking/entity/enums/SeatType.java), [`BookingStatus.java`](src/main/java/com/moviebooking/entity/enums/BookingStatus.java), [`ShowSeatStatus.java`](src/main/java/com/moviebooking/entity/enums/ShowSeatStatus.java), [`ShowStatus.java`](src/main/java/com/moviebooking/entity/enums/ShowStatus.java) | Stored as STRING in Postgres |
| **Custom exception hierarchy** | [`AppException.java`](src/main/java/com/moviebooking/exception/AppException.java) + 4 subclasses | Base carries `HttpStatus`; `GlobalExceptionHandler` maps each |
| **Global exception handler** | [`GlobalExceptionHandler.java`](src/main/java/com/moviebooking/exception/GlobalExceptionHandler.java) | `@RestControllerAdvice` — consistent `ApiResponse` error envelope |
| **Constructor injection** | Every `@Service` and `@Controller` | No `@Autowired` field injection |

---

## OOP / Design Patterns

| Concept | Where to find it | Notes |
|---------|-----------------|-------|
| **Strategy pattern** | [`PricingStrategy.java`](src/main/java/com/moviebooking/pricing/PricingStrategy.java) (interface) + 3 impls + [`PricingStrategyFactory.java`](src/main/java/com/moviebooking/pricing/PricingStrategyFactory.java) | Factory picks weekday/weekend/premium at runtime; `BookingService` calls the interface |
| **Polymorphism** | [`BookingService.java`](src/main/java/com/moviebooking/service/BookingService.java) `holdSeats()` line ~`pricingStrategyFactory.getStrategy(...).calculatePrice(...)` | Caller is agnostic to which concrete strategy executes |
| **Decorator pattern** | [`PremiumSeatPricingStrategy.java`](src/main/java/com/moviebooking/pricing/PremiumSeatPricingStrategy.java) | Wraps another `PricingStrategy` and adds 50% surcharge on top |
| **Composition over inheritance** | `PremiumSeatPricingStrategy` | Composed from a base strategy rather than extended |
| **Layered architecture** | Package structure: `controller → service → repository → entity` | Each layer has one responsibility; services own all business logic |

---

## Spring / JPA / DBMS Concepts

| Concept | Where to find it | Notes |
|---------|-----------------|-------|
| **Entity relationships** | [`Show.java`](src/main/java/com/moviebooking/entity/Show.java), [`ShowSeat.java`](src/main/java/com/moviebooking/entity/ShowSeat.java), [`Booking.java`](src/main/java/com/moviebooking/entity/Booking.java) | `@ManyToOne`, `@OneToMany`, `@OneToMany(cascade)` |
| **Unique constraints** | [`ShowSeat.java`](src/main/java/com/moviebooking/entity/ShowSeat.java), [`User.java`](src/main/java/com/moviebooking/entity/User.java) | `@UniqueConstraint` on `(show_id, seat_id)` |
| **Indexes** | [`Movie.java`](src/main/java/com/moviebooking/entity/Movie.java) (title, genre), [`Show.java`](src/main/java/com/moviebooking/entity/Show.java) (start_time), [`Booking.java`](src/main/java/com/moviebooking/entity/Booking.java) (expires_at), [`ShowSeat.java`](src/main/java/com/moviebooking/entity/ShowSeat.java) (held_at) | Chosen for query hot paths |
| **Derived query methods** | [`UserRepository.java`](src/main/java/com/moviebooking/repository/UserRepository.java), [`MovieRepository.java`](src/main/java/com/moviebooking/repository/MovieRepository.java) | Spring Data generates JPQL from method name |
| **Custom JPQL query** | [`MovieRepository.java`](src/main/java/com/moviebooking/repository/MovieRepository.java) `searchMovies()`, [`ShowRepository.java`](src/main/java/com/moviebooking/repository/ShowRepository.java) `findByTheaterCityAndStatusAfter()`, [`BookingRepository.java`](src/main/java/com/moviebooking/repository/BookingRepository.java) `findExpiredHeldBookings()` | `@Query` JPQL with named params and join traversal |
| **Native SQL query** | [`MovieRepository.java`](src/main/java/com/moviebooking/repository/MovieRepository.java) `findTopBookedMovies()` | `nativeQuery=true`; multi-table join + `GROUP BY` + `COUNT` + `LIMIT` |
| **Paginated, sortable listing** | [`MovieController.java`](src/main/java/com/moviebooking/controller/MovieController.java), [`BookingController.java`](src/main/java/com/moviebooking/controller/BookingController.java) | `Pageable` + `Sort` from request params |
| **@Transactional** | [`BookingService.java`](src/main/java/com/moviebooking/service/BookingService.java), all service methods | `readOnly=true` for queries; full read-write for mutations |
| **Transaction integrity** | [`BookingService.holdSeats()`](src/main/java/com/moviebooking/service/BookingService.java) | Booking + ShowSeat mutations are atomic; exception rolls everything back |
| **Bean Validation** | [`HoldSeatsRequest.java`](src/main/java/com/moviebooking/dto/request/HoldSeatsRequest.java), [`CreateUserRequest.java`](src/main/java/com/moviebooking/dto/request/CreateUserRequest.java) | `@NotNull`, `@NotEmpty`, `@Email`, `@Positive` on record components |
| **@PrePersist lifecycle hook** | [`User.java`](src/main/java/com/moviebooking/entity/User.java), [`Booking.java`](src/main/java/com/moviebooking/entity/Booking.java) | Auto-sets `createdAt` without service-layer boilerplate |
| **Seed data / DataInitializer** | [`DataInitializer.java`](src/main/java/com/moviebooking/config/DataInitializer.java) | `@EventListener(ApplicationReadyEvent)` with guard — idempotent on restart |

---

## Concurrency — THE CENTERPIECE

| Concept | Where to find it | Notes |
|---------|-----------------|-------|
| ⭐ **Pessimistic DB locking** | [`ShowSeatRepository.java`](src/main/java/com/moviebooking/repository/ShowSeatRepository.java) `findByIdsWithPessimisticLock()` | `@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE` in PostgreSQL |
| ⭐ **Lock callsite** | [`BookingService.java`](src/main/java/com/moviebooking/service/BookingService.java) `holdSeats()` | Locks row, validates AVAILABLE, writes HELD — all in one transaction |
| **Deadlock prevention** | [`ShowSeatRepository.java`](src/main/java/com/moviebooking/repository/ShowSeatRepository.java) | `ORDER BY ss.id ASC` inside the lock query — consistent ordering prevents cycle |
| **Optimistic locking alternative** | [`ShowSeat.java`](src/main/java/com/moviebooking/entity/ShowSeat.java) Javadoc | Explains `@Version` trade-offs (no retry needed with pessimistic; retry needed with optimistic) |
| **Seat hold TTL** | [`SeatHoldExpiryScheduler.java`](src/main/java/com/moviebooking/scheduler/SeatHoldExpiryScheduler.java) | Scheduled every 60s; releases HELD rows older than 5 min |
| **Java 21 Virtual Threads (Tomcat)** | [`application.yml`](src/main/resources/application.yml) `spring.threads.virtual.enabled: true` | All HTTP request threads are virtual |
| **Java 21 Virtual Threads (Scheduler)** | [`SchedulerConfig.java`](src/main/java/com/moviebooking/config/SchedulerConfig.java) | `Executors.newVirtualThreadPerTaskExecutor()` — scheduler tasks run on virtual threads |
| **Virtual threads in tests** | [`ConcurrentBookingTest.java`](src/test/java/com/moviebooking/booking/ConcurrentBookingTest.java) | `Thread.ofVirtual().start(...)` — 10 virtual threads race to book one seat |
| ⭐ **Concurrency integration test** | [`ConcurrentBookingTest.java`](src/test/java/com/moviebooking/booking/ConcurrentBookingTest.java) `onlyOneBookingShouldSucceedForSameSeat()` | **The lock proof**: 10 threads → exactly 1 succeeds, 9 get 409, seat = HELD |
| **Testcontainers (real PostgreSQL)** | [`ConcurrentBookingTest.java`](src/test/java/com/moviebooking/booking/ConcurrentBookingTest.java) | Must use real Postgres — H2 has different locking semantics |

---

## Spring AI — Agentic Tool Calling

| Concept | Where to find it | Notes |
|---------|-----------------|-------|
| **@Tool annotation** | [`BookingTools.java`](src/main/java/com/moviebooking/ai/BookingTools.java) | 5 `@Tool`-annotated methods; Spring AI 1.0.0 style — not `@Bean Function<I,O>` |
| **Tool registration** | [`BookingAssistantService.java`](src/main/java/com/moviebooking/ai/BookingAssistantService.java) | `MethodToolCallbackProvider.builder().toolObjects(bookingTools).build()` wired inline per request |
| **Tool implementations** | [`BookingTools.java`](src/main/java/com/moviebooking/ai/BookingTools.java) | 5 tools: searchMovies, listShows, checkSeatAvailability, holdSeats, confirmBooking |
| **Agentic chat loop** | [`BookingAssistantService.java`](src/main/java/com/moviebooking/ai/BookingAssistantService.java) | `chatClient.prompt().toolCallbacks(...).call()` — Gemini plans tool calls autonomously |
| **System prompt** | [`BookingAssistantService.java`](src/main/java/com/moviebooking/ai/BookingAssistantService.java) | Constrains LLM to only use DB-verified data; enforces tool call ordering |
| **AI routes through real service** | [`BookingTools.java`](src/main/java/com/moviebooking/ai/BookingTools.java) `holdSeats()` | Calls `bookingService.holdSeats(...)` — same pessimistic lock, same transactions |
| **Graceful degradation** | [`BookingAssistantService.java`](src/main/java/com/moviebooking/ai/BookingAssistantService.java) constructor | `chatClient` is null when `GEMINI_API_KEY` is absent; returns clear error message |
| **Endpoint** | [`BookingAssistantController.java`](src/main/java/com/moviebooking/ai/BookingAssistantController.java) | `POST /api/assistant` — accepts natural language, returns AI response |

---

## MCP (Model Context Protocol) Server

Spring AI 1.0.0 exposes the same five booking tools over MCP so any MCP-compatible client
(Claude Desktop, custom agents, IDE plugins) can orchestrate them without going through the REST API.

### Architecture

```
External MCP Client (e.g. Claude Desktop)
    │  SSE connection
    ▼
GET /mcp/sse  ─── Spring AI MCP Server (SSE transport, Servlet/SseEmitter)
POST /mcp/messages                │
                                  │  discovers ToolCallbackProvider bean
                          McpServerConfig.bookingMcpTools()
                                  │
                          BookingTools (@Tool methods)
                                  │
                          BookingService  ← same pessimistic lock + @Transactional
                                  │
                          PostgreSQL (SELECT … FOR UPDATE)
```

Key point: the concurrency guarantees from Priority 1 hold at every entry point — REST API,
agentic assistant, and MCP. Business logic lives in exactly one place.

### Tool Definitions

All tools are defined in [`BookingTools.java`](src/main/java/com/moviebooking/ai/BookingTools.java)
and registered for MCP via [`McpServerConfig.java`](src/main/java/com/moviebooking/ai/McpServerConfig.java).

| MCP Tool | Parameters | Description | Concurrency safety |
|----------|-----------|-------------|-------------------|
| `searchMovies` | `keyword: String` | Find movies by title or genre keyword | Read-only |
| `listShows` | `movieId: Long` | Upcoming shows for a movie | Read-only |
| `checkSeatAvailability` | `showId: Long` | Available seats with IDs, codes, types, prices | Read-only |
| `holdSeats` | `userId: Long`, `showId: Long`, `showSeatIds: List<Long>` | Reserve seats — IDs must come from `checkSeatAvailability` | **`SELECT FOR UPDATE` pessimistic lock** |
| `confirmBooking` | `bookingId: Long`, `userId: Long` | Finalise a held booking within 5-minute TTL | `@Transactional` |

### Connecting a client

**Claude Desktop** (`~/.claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "movie-booking": {
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

**curl smoke test** (lists registered MCP tools):
```bash
curl -N http://localhost:8080/mcp/sse &
curl -X POST http://localhost:8080/mcp/messages \
     -H 'Content-Type: application/json' \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

### Mirrors production MCP orchestration patterns

This follows the same layering used in production multi-agent systems: a domain service
exposed first as a REST API (Priority 1), then as an LLM-callable tool via an agentic
assistant (Priority 2), then as an MCP server so any compliant client can use it
(Priority 3) — without duplicating business logic at any layer.

---

## Deployment

| Concept | Where to find it |
|---------|-----------------|
| Multi-stage Docker build | [`Dockerfile`](Dockerfile) |
| JVM memory cap (Render 512MB) | [`Dockerfile`](Dockerfile) `ENV JAVA_TOOL_OPTIONS="-Xmx400m"` |
| Spring profiles (local / prod) | [`application-local.yml`](src/main/resources/application-local.yml), [`application-prod.yml`](src/main/resources/application-prod.yml) |
| Local dev one-command startup | [`docker-compose.yml`](docker-compose.yml) |
| Env-var only config (no hardcoding) | [`application-prod.yml`](src/main/resources/application-prod.yml) — `${SPRING_DATASOURCE_URL}` etc. |
| Dynamic PORT from Render | [`application.yml`](src/main/resources/application.yml) `server.port: ${PORT:8080}` |

---

## Future Scope (Planned — Not Yet Implemented)

### 1. JWT Authentication *(Priority 1)*

**Concept:** Stateless auth using signed tokens. User logs in → server issues a JWT → client sends it on every request as `Authorization: Bearer <token>`.

**Flow:**
```
POST /api/auth/register  → hash password (BCrypt), save user, return JWT
POST /api/auth/login     → verify password, return JWT
All other endpoints      → JwtFilter validates token, extracts userId from claims
```

**Key components:**
- `spring-boot-starter-security` + `jjwt` (io.jsonwebtoken)
- `JwtFilter extends OncePerRequestFilter` — validates token, sets `SecurityContext`
- `SecurityConfig` — whitelist `/api/auth/**`, `/swagger-ui/**`; protect everything else
- Extract `userId` from JWT claims instead of trusting it from the request body

**Why it matters:** Closes the "anyone can book as any user" security hole. Very common interview topic.

---

### 2. Redis Caching *(Priority 2)*

**Concept:** Cache expensive read queries in memory so repeated requests skip the database.

**What to cache:**
- `GET /api/movies` — movie list (invalidate on add/update)
- `GET /api/movies/{id}/shows` — shows for a movie
- `GET /api/shows/{id}/seats` — seat availability (short TTL ~30s, changes fast)

**Key components:**
- `spring-boot-starter-data-redis`
- `@EnableCaching` on main class
- `@Cacheable("movies")` on service methods
- `@CacheEvict` on write operations

**Hosting:** Upstash Redis — free tier, works with Render via `REDIS_URL` env var.

---

### 3. Prometheus + Grafana Monitoring *(Priority 3)*

**Concept:** Expose app metrics (request count, latency, JVM memory, DB pool) → scrape with Prometheus → visualize in Grafana.

**What to add (Actuator already present):**
- `micrometer-registry-prometheus` dependency
- Expose `/actuator/prometheus` in `application.yml`
- Custom metrics: bookings per minute, seat hold rate, AI tool call latency

**Hosting:** Grafana Cloud free tier (includes Prometheus scraping). Import Spring Boot dashboard ID: 12900.

---

### 4. Kafka *(Priority 4)*

**Concept:** Async event streaming. Booking actions publish events; consumers process them independently.

**Flow:**
```
BookingService.confirmBooking()
  → publish BookingConfirmedEvent to topic "booking-events"
    → NotificationConsumer  (email/SMS)
    → AnalyticsConsumer     (stats)
    → AuditConsumer         (audit log)
```

**Key components:**
- `spring-kafka`
- `BookingEventProducer` — publishes after confirm
- `BookingEventConsumer` — listens and processes
- Event schema: `{ bookingId, userId, movieTitle, showTime, seats, totalAmount, timestamp }`

**Hosting:** Upstash Kafka — free tier, serverless, works with Render env vars.

---

### 5. WebSockets — Real-time Seat Updates *(Priority 5)*

**Concept:** Push real-time seat availability to all users viewing a show — when someone holds a seat, everyone else sees it go unavailable instantly.

**Flow:**
```
User A holds seat A1
  → BookingService.holdSeats() completes
    → SeatUpdatePublisher pushes to /topic/show/{showId}
      → All connected frontends update their seat map
```

**Key components:**
- `spring-boot-starter-websocket`
- `WebSocketConfig` — STOMP over `/ws`, broker on `/topic`
- `SeatUpdatePublisher` — `SimpMessagingTemplate` called after hold/confirm/expire
- Frontend: SockJS + STOMP.js, subscribe to `/topic/show/{showId}`
