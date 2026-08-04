# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY backend/pom.xml backend/mvnw ./
COPY backend/.mvn .mvn

RUN ./mvnw dependency:go-offline -q

COPY backend/src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
