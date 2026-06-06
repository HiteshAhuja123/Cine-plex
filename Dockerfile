# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Cache Maven dependencies separately from source (faster rebuilds)
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Render free tier is 512MB RAM — cap the JVM to avoid OOM kills.
# -Xmx400m leaves ~100MB for the OS and off-heap allocations.
# -XX:+UseG1GC performs well at low heap sizes.
# -XX:+UseContainerSupport respects cgroup memory limits automatically.
ENV JAVA_TOOL_OPTIONS="-Xmx400m -XX:+UseG1GC -XX:+UseContainerSupport"

COPY --from=builder /app/target/movie-booking-*.jar app.jar

# Render injects PORT; fall back to 8080 for Docker Compose / local testing
EXPOSE ${PORT:-8080}

# Run as non-root for security best practice
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
