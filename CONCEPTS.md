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
| **Tool registration** | [`BookingToolsConfig.java`](src/main/java/com/moviebooking/ai/BookingToolsConfig.java) | `FunctionCallbackWrapper.builder(fn).withName(...).withDescription(...)` — LLM receives schema |
| **Tool implementations** | [`BookingTools.java`](src/main/java/com/moviebooking/ai/BookingTools.java) | 5 tools: searchMovies, listShows, checkSeatAvailability, holdSeats, confirmBooking |
| **Agentic chat loop** | [`BookingAssistantService.java`](src/main/java/com/moviebooking/ai/BookingAssistantService.java) | `chatClient.prompt().functions(...).call()` — Gemini plans tool calls autonomously |
| **System prompt** | [`BookingAssistantService.java`](src/main/java/com/moviebooking/ai/BookingAssistantService.java) | Constrains LLM to only use DB-verified data; enforces tool call ordering |
| **AI routes through real service** | [`BookingTools.java`](src/main/java/com/moviebooking/ai/BookingTools.java) `holdSeatsFn()` | Calls `bookingService.holdSeats(...)` — same pessimistic lock, same transactions |
| **Graceful degradation** | [`BookingAssistantService.java`](src/main/java/com/moviebooking/ai/BookingAssistantService.java) constructor | `chatClient` is null when `GEMINI_API_KEY` is absent; returns clear error message |
| **Endpoint** | [`BookingAssistantController.java`](src/main/java/com/moviebooking/ai/BookingAssistantController.java) | `POST /api/assistant` — accepts natural language, returns AI response |

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
