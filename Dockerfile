# ── Build stage ─────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn package -DskipTests -B

# ── Runtime stage ───────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S pfe && adduser -S pfe -G pfe

COPY --from=builder /app/target/*.jar app.jar
RUN chown pfe:pfe app.jar

USER pfe
EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
