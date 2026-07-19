# --- Build stage ---
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /build

# Cache de dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:23-jre-jammy
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /build/target/*.war app.war
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

ENV UPLOADS_DIR=/app/uploads
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war"]
