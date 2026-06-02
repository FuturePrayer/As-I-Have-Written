# syntax=docker/dockerfile:1

FROM maven:3.9.15-eclipse-temurin-21 AS build
WORKDIR /workspace

ARG PROJECT_VERSION=0.0.3

COPY docker-log-agent/pom.xml docker-log-agent/pom.xml
COPY docker-log-agent/src docker-log-agent/src
RUN mvn -B -f docker-log-agent/pom.xml -Drevision="${PROJECT_VERSION}" -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --home-dir /app appuser
COPY --from=build --chown=appuser:appuser /workspace/docker-log-agent/target/*.jar /app/app.jar

USER appuser

ENV JAVA_OPTS=""
LABEL aih.logs.agent="true"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
