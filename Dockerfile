# --- Build stage ---
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /build

# Cache de dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:23-jre-noble
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /build/target/*.war app.war
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

ENV UPLOADS_DIR=/app/uploads
# Sin esto, la JVM en contenedores Linux minimalistas usa /dev/random para
# SecureRandom (BCrypt, Tomcat session IDs, UUID.randomUUID) y se puede quedar
# colgada indefinidamente esperando entropía, sin log ni excepción: eso es lo
# que causaba que el arranque se congelara justo después del escaneo de
# repositorios JPA y que el login/registro fallara de forma intermitente.
ENV JAVA_TOOL_OPTIONS="-Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war"]
