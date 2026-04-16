# ── Build stage ───────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

# ── Runtime stage ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S pfe && adduser -S pfe -G pfe

COPY --from=builder /app/target/*.jar app.jar
RUN chown pfe:pfe app.jar

USER pfe
EXPOSE 8081

ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]# ── Build stage ───────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copier tout ce qui est nécessaire à Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# ── Runtime stage ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S pfe && adduser -S pfe -G pfe

COPY --from=builder /app/target/*.jar app.jar
RUN chown pfe:pfe app.jar

USER pfe
EXPOSE 8081

ENTRYPOINT [
  "java",
  "-XX:+UseContainerSupport",
  "-XX:MaxRAMPercentage=75.0",
  "-Djava.security.egd=file:/dev/./urandom",
  "-jar",
  "app.jar"
]
