# syntax=docker/dockerfile:1
ARG SERVICE

FROM eclipse-temurin:21-jdk-jammy AS build
ARG SERVICE
WORKDIR /workspace

COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY common common
COPY contracts contracts
COPY movie-service movie-service
COPY people-service people-service

RUN chmod +x gradlew \
    && ./gradlew ":${SERVICE}:bootJar" --no-daemon \
    && JAR=$(find "${SERVICE}/build/libs" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit) \
    && test -n "$JAR" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080 8081 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]