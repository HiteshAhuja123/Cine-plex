# 🎬 Movie Ticket Booking System

<!-- TODO: Add Swagger screenshot here -->
![Swagger UI Screenshot](docs/swagger-screenshot.png)

<!-- TODO: Replace with Loom / GIF demo link -->
> 📽️ **[Demo video — click to watch](https://loom.com/YOUR-DEMO-LINK)**

> ⚠️ **Free-tier note**: The live demo runs on Render's free instance. The first request after a period of inactivity may take **up to 30 seconds** while the instance wakes up. Subsequent requests are fast.

**Live demo**: [https://YOUR-APP-NAME.onrender.com/swagger-ui.html](https://YOUR-APP-NAME.onrender.com/swagger-ui.html)

---

## Overview

A **production-quality Java backend** for a movie ticket booking platform, built as a portfolio project to demonstrate backend depth across:

| Area | What's shown |
|------|-------------|
| **Spring Boot** | 3.x layered architecture (controller / service / repository / entity / dto / mapper / exception) |
| **DBMS** | PostgreSQL with Hibernate ORM, derived queries, custom JPQL, native SQL aggregation, paginated sortable listings |
| **Concurrency** | DB-level pessimistic locking (`SELECT FOR UPDATE`) preventing double-booking; scheduler with Java 21 virtual threads |
| **Clean OOP** | Strategy pattern (pricing), custom exception hierarchy, generics (`ApiResponse<T>`), records for DTOs |
| **Java 21** | Virtual threads, records, switch expressions, pattern matching, `java.time` |
| **AI** | Agentic assistant via Spring AI + Google Gemini — the LLM plans and calls booking tools autonomously |
| **Testing** | Testcontainers (real PostgreSQL), JUnit 5, Mockito — including a concurrent-booking lock proof test |
| **Deployment** | Multi-stage Docker, Render (app) + Neon (Postgres), Spring profiles |

---

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                        HTTP Clients                           │
└──────────────────────────┬────────────────────────────────────┘
                           │ REST / JSON
┌──────────────────────────▼────────────────────────────────────┐
│  Controllers  (MovieController, ShowController,               │
│               BookingController, UserController,              │
│               BookingAssistantController)                     │
└──────────────────────────┬────────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────────┐
│  Services  (BookingService ← PESSIMISTIC LOCK HERE,           │
│             MovieService, ShowService, UserService)           │
│  Pricing   (PricingStrategyFactory → Strategy impls)          │
│  Scheduler (SeatHoldExpiryScheduler — virtual threads)        │
│  AI        (BookingAssistantService → Gemini + tools)         │
└──────────────────────────┬────────────────────────────────────┘
                           │ JPA/Hibernate
┌──────────────────────────▼────────────────────────────────────┐
│  Repositories  (ShowSeatRepository.findByIdsWithPessimisticLock│
│                MovieRepository.findTopBookedMovies [native])   │
└──────────────────────────┬────────────────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────────────────┐
│  PostgreSQL (Neon in prod, Docker in dev)                     │
└───────────────────────────────────────────────────────────────┘
```

---

## ER Diagram

```mermaid
erDiagram
    User {
        bigint id PK
        string name
        string email UK
        string phone
    }
    Movie {
        bigint id PK
        string title IDX
        string genre IDX
        string language
        date releaseDate
        boolean active
    }
    Theater {
        bigint id PK
        string name
        string city IDX
    }
    Screen {
        bigint id PK
        bigint theater_id FK
        string name
        int totalSeats
    }
    Seat {
        bigint id PK
        bigint screen_id FK
        string rowLabel
        int columnNumber
        string seatCode
        enum seatType "REGULAR|PREMIUM"
    }
    Show {
        bigint id PK
        bigint movie_id FK
        bigint screen_id FK
        datetime startTime IDX
        decimal basePrice
        string language
        enum status "ACTIVE|CANCELLED"
    }
    ShowSeat {
        bigint id PK
        bigint show_id FK
        bigint seat_id FK
        enum status "AVAILABLE|HELD|BOOKED"
        datetime heldAt IDX
        bigint heldByBookingId
        decimal price
    }
    Booking {
        bigint id PK
        bigint user_id FK
        bigint show_id FK
        enum status "HELD|CONFIRMED|CANCELLED|EXPIRED"
        decimal totalAmount
        datetime expiresAt IDX
    }
    BookingSeat {
        bigint id PK
        bigint booking_id FK
        bigint show_seat_id FK
    }

    Theater ||--o{ Screen : "has"
    Screen  ||--o{ Seat   : "contains"
    Movie   ||--o{ Show   : "screened as"
    Screen  ||--o{ Show   : "hosts"
    Show    ||--o{ ShowSeat : "per-seat state"
    Seat    ||--o{ ShowSeat : "tracked in"
    User    ||--o{ Booking  : "places"
    Show    ||--o{ Booking  : "covered by"
    Booking ||--o{ BookingSeat : "includes"
    ShowSeat ||--o{ BookingSeat : "linked to"
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/users` | Register a user |
| `GET` | `/api/movies?keyword=&page=&size=&sort=` | Browse movies (paginated) |
| `GET` | `/api/movies/{id}` | Get movie details |
| `GET` | `/api/movies/top-booked?limit=5` | Top N most-booked (native SQL aggregation) |
| `GET` | `/api/shows/movie/{movieId}` | Shows for a movie |
| `GET` | `/api/shows/{showId}/seats` | Full seat map with status |
| `POST` | `/api/bookings/hold` | **Hold seats** (pessimistic lock) |
| `POST` | `/api/bookings/{id}/confirm` | Confirm a hold |
| `POST` | `/api/bookings/{id}/cancel` | Cancel a booking |
| `GET` | `/api/bookings/user/{userId}` | Booking history |
| `POST` | `/api/assistant` | **AI agentic booking** (natural language) |

Full interactive docs: `/swagger-ui.html`

---

## Run Locally

### Prerequisites
- Docker Desktop running
- Java 21+ (only needed if running without Docker)

### With Docker Compose (recommended)

```bash
# 1. Clone
git clone https://github.com/YOUR-USERNAME/movie-booking.git
cd movie-booking

# 2. (Optional) Set your Gemini API key for AI features
export GEMINI_API_KEY=your_key_here

# 3. Start everything
docker compose up --build

# App starts at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Without Docker (Maven)

```bash
# Requires a local PostgreSQL at localhost:5432 (see application-local.yml)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Run Tests

```bash
# Starts a Testcontainers PostgreSQL automatically — Docker must be running
./mvnw test
```

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | **Prod** | JDBC URL — e.g. `jdbc:postgresql://...neon.tech/...?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | **Prod** | Neon DB username |
| `SPRING_DATASOURCE_PASSWORD` | **Prod** | Neon DB password |
| `GEMINI_API_KEY` | Optional | Google AI Studio key. App runs without it; `/api/assistant` returns a graceful message. |
| `PORT` | Render-injected | HTTP port (defaults to 8080) |
| `SPRING_PROFILES_ACTIVE` | Optional | `local` (default) or `prod` |

---

## Deployment (Render + Neon)

### 1. Create a Neon database
1. Sign up at [neon.tech](https://neon.tech) (free tier)
2. Create a new project → copy the **Connection string** (format: `postgres://user:pass@host/db?sslmode=require`)
3. Convert to JDBC: `jdbc:postgresql://host/db?sslmode=require`

### 2. Deploy on Render
1. Sign up at [render.com](https://render.com)
2. New → **Web Service** → connect your GitHub repo
3. Settings:
   - **Environment**: Docker
   - **Dockerfile path**: `./Dockerfile`
4. Add environment variables:
   ```
   SPRING_PROFILES_ACTIVE = prod
   SPRING_DATASOURCE_URL  = jdbc:postgresql://<neon-host>/<db>?sslmode=require
   SPRING_DATASOURCE_USERNAME = <neon-user>
   SPRING_DATASOURCE_PASSWORD = <neon-password>
   GEMINI_API_KEY = <your-key>   # optional
   ```
5. Deploy → Render will build the multi-stage Docker image

### 3. Verify
- Health check: `GET /actuator/health`
- Swagger: `/swagger-ui.html`
- Demo data is seeded automatically on first boot

---

## Stack

- **Java 21** (LTS) — virtual threads, records, switch expressions
- **Spring Boot 3.3.5** — web, data-jpa, validation, actuator
- **PostgreSQL 16** — via Neon (prod) / Docker (dev)
- **Spring AI 1.0.0** — Google Gemini tool-calling
- **springdoc-openapi 2.5.0** — Swagger UI
- **JUnit 5 + Mockito + Testcontainers** — unit and integration tests
