# syntax=docker/dockerfile:1

FROM maven:3.9.15-eclipse-temurin-26 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:26-jre
WORKDIR /app

RUN useradd --system --create-home --home-dir /app appuser
COPY --from=build --chown=appuser:appuser /workspace/target/*.jar /app/app.jar

USER appuser
EXPOSE 25091

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
