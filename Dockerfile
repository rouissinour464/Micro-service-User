FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S pfe && adduser -S pfe -G pfe

COPY target/*.jar app.jar
RUN chown pfe:pfe app.jar

USER pfe
EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
