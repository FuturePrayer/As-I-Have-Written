<p align="center">
  <img src="logo.png" alt="Peony" width="128" height="128">
</p>

<h1 align="center">As I Have Written</h1>

<p align="center">
<strong>As I Have Written is a lightweight Spring Boot 4 log ingestion and query console backed by MongoDB. It accepts logs through HTTP, batch HTTP, NDJSON streamable HTTP, and a pluggable MQ receiver. It tokenizes logs in the application, stores them in MongoDB, and provides a WebUI for service-scoped querying, API key management, custom metadata filters, and Chinese/English switching.</strong>
</p>

---

[简体中文](README.md) | English

[![Release](https://img.shields.io/github/v/release/FuturePrayer/As-I-Have-Written?sort=semver)](https://github.com/FuturePrayer/As-I-Have-Written/releases)
[![Java 21](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE.txt)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/FuturePrayer/As-I-Have-Written)

## Features

- Spring Boot 4 with virtual threads enabled by default.
- MongoDB persistence with application-side tokenization.
- Single-log HTTP, batch HTTP, and NDJSON streamable HTTP ingestion.
- Pluggable MQ receiver interface with a JSON test receiver.
- WebUI sessions powered by Sa-Token, without Spring Security.
- API keys scoped to `Service + Instance`; persisted log ownership comes from the API key binding.
- API keys are hash-checked for authentication and AES-GCM encrypted for repeatable WebUI display.
- WebUI service selection, log filters, API key management, custom metadata filters, and Chinese/English switching.
- Default WebUI query range is the last 15 minutes; once services exist, WebUI queries require a service.

## Tech Stack

- Java 21+
- Spring Boot 4
- Spring Web MVC
- Thymeleaf
- Sa-Token
- MongoDB
- Maven

## Quick Start

### Docker Compose with MongoDB

Use this for local evaluation. Compose builds the application image from the current source tree; MongoDB is only exposed inside the Compose network, and data is stored in a Docker volume.

```bash
AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret' \
AIH_ADMIN_PASSWORD='replace-this-password' \
AIH_DEFAULT_API_KEY='replace-this-default-ingest-key' \
docker compose up -d
```

Open the WebUI:

```text
http://localhost:25091/ui/logs
```

### Docker Compose with an External MongoDB

Use the external MongoDB Compose file when MongoDB is already available. Compose builds the application image from the current source tree:

```bash
MONGODB_URI='mongodb://user:password@mongo-host:27017/as_i_have_written?authSource=admin' \
AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret' \
AIH_ADMIN_PASSWORD='replace-this-password' \
AIH_DEFAULT_API_KEY='replace-this-default-ingest-key' \
docker compose -f docker-compose.external-mongodb.yml up -d
```

### Run from Source

Requirements:

- JDK 21 or newer.
- Maven 3.9 or newer.
- MongoDB 6 or a compatible MongoDB deployment.

Start the application:

```bash
export MONGODB_URI='mongodb://localhost:27017/as_i_have_written'
export AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret'
export AIH_ADMIN_USERNAME='admin'
export AIH_ADMIN_PASSWORD='replace-this-password'
export AIH_DEFAULT_API_KEY='replace-this-default-ingest-key'

mvn spring-boot:run
```

The login user is controlled by `AIH_ADMIN_USERNAME` and `AIH_ADMIN_PASSWORD`. If they are not set, the development default is `admin / admin123`; change it in shared and production environments.

## Docker Image

The release workflow publishes images to GitHub Container Registry:

```text
ghcr.io/futureprayer/as-i-have-written:<version>
ghcr.io/futureprayer/as-i-have-written:latest
ghcr.io/futureprayer/as-i-have-written-docker-agent:<version>
ghcr.io/futureprayer/as-i-have-written-docker-agent:latest
```

Users in mainland China can also use the accelerated mirror:

```text
swr.cn-east-3.myhuaweicloud.com/suhoan/as-i-have-written:latest
```

You can also build an image locally:

```bash
docker build -t as-i-have-written:local .
docker build -f docker-log-agent/Dockerfile -t as-i-have-written-docker-agent:local .
```

## Configuration

The application reads `src/main/resources/application.yml` and environment variables.

| Environment variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `25091` | HTTP server port. |
| `SERVER_HTTP2_ENABLED` | `false` | Enables HTTP/2; without TLS this uses h2c, which is usually best behind an h2c-capable reverse proxy. |
| `MONGODB_URI` | `mongodb://localhost:27017/as_i_have_written` | MongoDB connection URI. |
| `AIH_ADMIN_USERNAME` | `admin` | WebUI administrator username. |
| `AIH_ADMIN_PASSWORD` | `admin123` | WebUI administrator password. Change this outside local development. |
| `AIH_API_KEY_ENCRYPTION_KEY` | none | Required. AES-GCM key material used to encrypt API keys. Startup fails if missing. |
| `AIH_INGEST_QUEUE_CAPACITY` | `10000` | In-memory ingestion queue capacity. |
| `AIH_INGEST_BATCH_SIZE` | `500` | Writer batch size. |
| `AIH_INGEST_FLUSH_INTERVAL` | `1s` | Writer flush interval. |
| `AIH_DEFAULT_SERVICE_NAME` | `default` | Default service created on startup. |
| `AIH_DEFAULT_SERVICE_DISPLAY_NAME` | `Default` | Display name for the default service. |
| `AIH_DEFAULT_INSTANCE_NAME` | `default` | Default instance created on startup. |
| `AIH_DEFAULT_API_KEY` | `dev-api-key` | Default API key created on startup. Change this outside local development. |
| `AIH_SERVICE_CLEANUP_FIXED_DELAY` | `10m` | Interval for automatically removing empty services. |
| `AIH_SERVICE_CLEANUP_INITIAL_DELAY` | `10m` | Initial delay before the first empty-service cleanup run. |

MongoDB collections:

- `log_entries`: persisted logs.
- `log_services`: service definitions used by the WebUI selector.
- `log_sources`: API key bindings for `serviceName + instanceName`.

Spring Data MongoDB creates indexes automatically. MongoDB stores application-generated `tokens` and performs ordinary indexed matching; it does not perform full-text tokenization.

## Performance Tuning

- Set `SERVER_HTTP2_ENABLED=true` to let h2c-capable or TLS HTTP/2 clients reuse connections and reduce connection overhead during high-concurrency batch ingestion. The Docker Log Agent's HTTP client prefers HTTP/2 and falls back to HTTP/1.1 when the server or proxy does not support it.
- Prefer `/api/logs/batch` or NDJSON stream ingestion. Avoid high-frequency single-log calls to `/api/logs`.
- Server throughput is mostly shaped by `AIH_INGEST_BATCH_SIZE`, `AIH_INGEST_FLUSH_INTERVAL`, `AIH_INGEST_QUEUE_CAPACITY`, and MongoDB write capacity. Increase batch and queue sizes for heavier traffic; shorten the flush interval when lower latency matters more.
- Docker Log Agent defaults to `AIH_AGENT_BATCH_SIZE=500`, matching the server writer batch size. For high-throughput multi-container hosts, increase `AIH_AGENT_QUEUE_CAPACITY`; larger queues also mean more Agent memory usage.
- Keep MongoDB in the same region or on a low-latency network, and reserve enough IOPS for the database behind `log_entries`. WebUI query performance depends on indexes and time range, so keep the default query window short in production.

## Ingestion API

All ingestion endpoints require:

```http
X-API-Key: <api-key>
```

`X-Log-Source` is deprecated and ignored by the current implementation.

The request body still contains a `service` field for validation and tokenization compatibility. Persisted `service` and `instanceName` always come from the API key binding, so clients cannot spoof ownership by changing the payload.

### Single Log

```bash
curl -i -X POST 'http://localhost:25091/api/logs' \
  -H 'X-API-Key: <api-key>' \
  -H 'Content-Type: application/json' \
  -d '{
    "service": "client-value",
    "environment": "prod",
    "level": "INFO",
    "message": "single log",
    "metadata": {
      "threadName": "main"
    }
  }'
```

### Batch Logs

```bash
curl -i -X POST 'http://localhost:25091/api/logs/batch' \
  -H 'X-API-Key: <api-key>' \
  -H 'Content-Type: application/json' \
  -d '[
    {"service":"client-value","environment":"prod","level":"INFO","message":"one"},
    {"service":"client-value","environment":"prod","level":"WARN","message":"two"}
  ]'
```

### NDJSON Streamable HTTP

```bash
curl -i -X POST 'http://localhost:25091/api/logs/stream' \
  -H 'X-API-Key: <api-key>' \
  -H 'Content-Type: application/x-ndjson' \
  --data-binary $'{"service":"client-value","environment":"prod","level":"INFO","message":"one"}\n{"service":"client-value","environment":"prod","level":"WARN","message":"two"}\n'
```

Responses:

| Status | Meaning |
| --- | --- |
| `202 Accepted` | Logs were accepted into the in-memory queue. |
| `400 Bad Request` | Payload validation failed. |
| `401 Unauthorized` | API key is missing, invalid, or disabled. |
| `429 Too Many Requests` | Ingestion queue is full. |

## Integrating from Another Spring Boot Project

If another application uses Spring Boot's default SLF4J + Logback stack, add a custom Logback appender that sends logs to this project asynchronously in batches:

```text
POST http://<aih-host>:25091/api/logs/batch
X-API-Key: <api-key>
```

The request body must still include `service`, `environment`, `level`, and `message`. The `service` field is kept for validation and tokenization compatibility; persisted ownership still comes from the service and instance bound to `X-API-Key`.

A regular Spring Boot Web application already includes Logback and Jackson. Non-Web applications should keep logging and JSON support:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-json</artifactId>
</dependency>
```

Place the following class in the client application, for example as `com.example.logging.AihLogbackAppender`. The example uses the default Jackson 2 package name from Spring Boot 3.x; for Spring Boot 4 / Jackson 3, change the `ObjectMapper` import to `tools.jackson.databind.ObjectMapper`.

```java
package com.example.logging;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AihLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private BlockingQueue<Map<String, Object>> queue;
    private HttpClient httpClient;
    private Thread worker;

    private String endpoint;
    private String apiKey;
    private String service = "application";
    private String environment = "local";
    private int batchSize = 100;
    private int queueCapacity = 10000;
    private long flushIntervalMillis = 1000;
    private long requestTimeoutMillis = 5000;
    private long connectTimeoutMillis = 3000;

    @Override
    public void start() {
        if (isBlank(endpoint)) {
            addError("endpoint is required");
            return;
        }
        if (isBlank(apiKey)) {
            addError("apiKey is required");
            return;
        }
        if (isBlank(service)) {
            addError("service is required");
            return;
        }
        if (isBlank(environment)) {
            addError("environment is required");
            return;
        }

        batchSize = Math.max(1, batchSize);
        queueCapacity = Math.max(batchSize, queueCapacity);
        flushIntervalMillis = Math.max(100, flushIntervalMillis);
        requestTimeoutMillis = Math.max(1000, requestTimeoutMillis);
        connectTimeoutMillis = Math.max(1000, connectTimeoutMillis);

        queue = new ArrayBlockingQueue<>(queueCapacity);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .build();

        running.set(true);
        worker = new Thread(this::writeLoop, "aih-logback-appender");
        worker.setDaemon(true);
        worker.start();

        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        Map<String, Object> payload = toPayload(event);
        if (!queue.offer(payload)) {
            addWarn("As I Have Written log queue is full; dropping log event.");
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        super.stop();
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        flushRemaining();
    }

    private void writeLoop() {
        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        long flushAtNanos = 0L;

        while (running.get()) {
            try {
                if (batch.isEmpty()) {
                    Map<String, Object> first = queue.poll(flushIntervalMillis, TimeUnit.MILLISECONDS);
                    if (first == null) {
                        continue;
                    }
                    batch.add(first);
                    flushAtNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(flushIntervalMillis);
                }

                queue.drainTo(batch, Math.max(0, batchSize - batch.size()));
                long waitNanos = flushAtNanos - System.nanoTime();
                if (batch.size() < batchSize && waitNanos > 0) {
                    Map<String, Object> next = queue.poll(waitNanos, TimeUnit.NANOSECONDS);
                    if (next != null) {
                        batch.add(next);
                        queue.drainTo(batch, Math.max(0, batchSize - batch.size()));
                    }
                }

                if (batch.size() >= batchSize || System.nanoTime() >= flushAtNanos) {
                    sendBatch(batch);
                    batch.clear();
                    flushAtNanos = 0L;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ex) {
                addWarn("Failed to process As I Have Written log batch.", ex);
                batch.clear();
                flushAtNanos = 0L;
            }
        }
    }

    private void flushRemaining() {
        if (queue == null || queue.isEmpty()) {
            return;
        }
        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch);
        sendBatch(batch);
    }

    private void sendBatch(List<Map<String, Object>> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(batch);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMillis(requestTimeoutMillis))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                addWarn("As I Have Written returned HTTP " + response.statusCode() + ": " + trim(response.body()));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            addWarn("Failed to send logs to As I Have Written.", ex);
        }
    }

    private Map<String, Object> toPayload(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventTime", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        payload.put("service", service);
        payload.put("environment", environment);
        payload.put("level", event.getLevel().toString());
        payload.put("traceId", blankToNull(mdc == null ? null : mdc.get("traceId")));
        payload.put("spanId", blankToNull(mdc == null ? null : mdc.get("spanId")));
        payload.put("message", messageWithThrowable(event));

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("loggerName", event.getLoggerName());
        metadata.put("threadName", event.getThreadName());
        if (mdc != null && !mdc.isEmpty()) {
            metadata.put("mdc", new LinkedHashMap<>(mdc));
        }
        if (event.getThrowableProxy() != null) {
            metadata.put("exceptionClass", event.getThrowableProxy().getClassName());
        }
        payload.put("metadata", metadata);
        return payload;
    }

    private String messageWithThrowable(ILoggingEvent event) {
        StringBuilder message = new StringBuilder(String.valueOf(event.getFormattedMessage()));
        appendThrowable(message, event.getThrowableProxy(), "");
        return message.toString();
    }

    private void appendThrowable(StringBuilder message, IThrowableProxy throwable, String prefix) {
        if (throwable == null) {
            return;
        }
        message.append(System.lineSeparator())
                .append(prefix)
                .append(throwable.getClassName());
        if (throwable.getMessage() != null) {
            message.append(": ").append(throwable.getMessage());
        }
        for (StackTraceElementProxy element : throwable.getStackTraceElementProxyArray()) {
            message.append(System.lineSeparator()).append("    at ").append(element.getSTEAsString());
        }
        appendThrowable(message, throwable.getCause(), "Caused by: ");
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 512 ? value : value.substring(0, 512) + "...";
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public void setFlushIntervalMillis(long flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public void setRequestTimeoutMillis(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }
}
```

Configure the appender in the client application's `src/main/resources/logback-spring.xml`:

```xml
<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

  <springProperty scope="context" name="aihLogEndpoint" source="AIH_LOG_ENDPOINT"
                  defaultValue="http://localhost:25091/api/logs/batch"/>
  <springProperty scope="context" name="aihLogApiKey" source="AIH_LOG_API_KEY"/>
  <springProperty scope="context" name="applicationName" source="spring.application.name"
                  defaultValue="client-service"/>
  <springProperty scope="context" name="environment" source="spring.profiles.active"
                  defaultValue="local"/>

  <appender name="AIH" class="com.example.logging.AihLogbackAppender">
    <endpoint>${aihLogEndpoint}</endpoint>
    <apiKey>${aihLogApiKey}</apiKey>
    <service>${applicationName}</service>
    <environment>${environment}</environment>
    <batchSize>100</batchSize>
    <queueCapacity>10000</queueCapacity>
    <flushIntervalMillis>1000</flushIntervalMillis>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="AIH"/>
  </root>
</configuration>
```

Set these variables before starting the client application:

```bash
export AIH_LOG_ENDPOINT='http://localhost:25091/api/logs/batch'
export AIH_LOG_API_KEY='<api-key>'
export SPRING_APPLICATION_NAME='payment-service'
export SPRING_PROFILES_ACTIVE='prod'
```

This sample appender does not persist or retry failed sends. If the remote endpoint is unavailable, returns non-2xx, or the queue is full, logs are dropped while local console output remains available. For stronger delivery guarantees, use MQ or a client-side durable buffer.

## Docker Container Log Agent

To collect stdout/stderr logs from Docker containers, use the built-in Docker Log Agent. It collects container runtime logs, not historical logs from the image itself. The Agent discovers containers through the Docker socket and only collects containers that explicitly opt in:

```text
aih.logs.enabled=true
```

The Agent resolves each container's `aih.logs.api-key-ref` to a real API key, then writes logs through `/api/logs/batch`. Do not put plaintext API keys in Docker labels; labels are visible through the Docker API and operational tooling.

### Agent Configuration

| Environment variable | Default | Description |
| --- | --- | --- |
| `AIH_AGENT_ENDPOINT` | `http://localhost:25091/api/logs/batch` | Batch ingestion endpoint. |
| `AIH_AGENT_API_KEY_FILE` | `/run/secrets/aih-agent-api-keys.properties` | API key reference file in Java properties format. |
| `AIH_AGENT_API_KEYS` | empty | Quick inline config, formatted as `ref1=key1;ref2=key2`; file config has higher priority. |
| `AIH_AGENT_DEFAULT_ENVIRONMENT` | `local` | Environment used when a container does not provide one. |
| `AIH_AGENT_LOG_REGEX` | empty | Global log parsing regex using Java named groups. |
| `AIH_AGENT_LOG_REGEX_METADATA_GROUPS` | empty | Extra regex group names to copy into metadata, comma-separated. |
| `AIH_AGENT_BATCH_SIZE` | `500` | Batch send size. |
| `AIH_AGENT_QUEUE_CAPACITY` | `10000` | Outbound queue capacity. |
| `AIH_AGENT_FLUSH_INTERVAL` | `1s` | Batch flush interval. |
| `AIH_AGENT_REQUEST_TIMEOUT` | `5s` | Ingestion request timeout. |
| `AIH_AGENT_DOCKER_HOST` | empty | Optional Docker Engine endpoint; when empty, docker-java uses its default discovery rules. |

Container labels:

| Label | Description |
| --- | --- |
| `aih.logs.enabled=true` | Enable collection. |
| `aih.logs.api-key-ref=<ref>` | Required. Resolves the real key from the Agent API key map. |
| `aih.logs.service=<service>` | Optional. Defaults to Compose service, then container name. |
| `aih.logs.instance=<instance>` | Optional. Defaults to container name. |
| `aih.logs.environment=<environment>` | Optional. Defaults to `AIH_AGENT_DEFAULT_ENVIRONMENT`. |
| `aih.logs.regex=<regex>` | Optional. Overrides the global regex. |
| `aih.logs.regex-metadata-groups=<names>` | Optional. Overrides global metadata groups. |

Regex parsing recognizes these built-in named groups: `time`, `level`, `traceId`, `spanId`, and `message`. `time` must be ISO-8601; `level` accepts only `TRACE`, `DEBUG`, `INFO`, `WARN`, and `ERROR`. Without a regex, or when a line does not match, the full line becomes `message`, stdout maps to `INFO`, and stderr maps to `ERROR`.

### Use with docker run

Create an API key reference file:

```properties
payment=replace-with-payment-api-key
order=replace-with-order-api-key
```

Start the Agent:

```bash
docker run -d --name aih-docker-agent \
  --add-host=host.docker.internal:host-gateway \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v "$PWD/aih-agent-api-keys.properties:/run/secrets/aih-agent-api-keys.properties:ro" \
  -e AIH_AGENT_ENDPOINT='http://host.docker.internal:25091/api/logs/batch' \
  -e AIH_AGENT_DEFAULT_ENVIRONMENT='prod' \
  -e AIH_AGENT_LOG_REGEX='^(?<time>\S+) (?<level>\w+) trace=(?<traceId>\S+) span=(?<spanId>\S+) (?<message>.*)$' \
  ghcr.io/futureprayer/as-i-have-written-docker-agent:latest
```

Start an application container to collect:

```bash
docker run -d --name payment-api \
  --label aih.logs.enabled=true \
  --label aih.logs.api-key-ref=payment \
  --label aih.logs.service=payment \
  --label aih.logs.instance=payment-api-1 \
  --label aih.logs.environment=prod \
  example/payment-api:latest
```

### Use with Docker Compose

```yaml
services:
  aih-agent:
    image: ghcr.io/futureprayer/as-i-have-written-docker-agent:latest
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      AIH_AGENT_ENDPOINT: http://host.docker.internal:25091/api/logs/batch
      AIH_AGENT_DEFAULT_ENVIRONMENT: prod
      AIH_AGENT_LOG_REGEX: '^(?<time>\S+) (?<level>\w+) trace=(?<traceId>\S+) span=(?<spanId>\S+) (?<message>.*)$'
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./aih-agent-api-keys.properties:/run/secrets/aih-agent-api-keys.properties:ro
    labels:
      aih.logs.agent: "true"
    restart: unless-stopped

  payment-api:
    image: example/payment-api:latest
    labels:
      aih.logs.enabled: "true"
      aih.logs.api-key-ref: payment
      aih.logs.service: payment
      aih.logs.instance: payment-api-1
      aih.logs.environment: prod
```

The Agent writes `containerId`, `containerName`, `image`, `stream`, `composeProject`, `composeService`, and safe Docker labels into metadata. It follows only new logs after startup, does not backfill history, and does not persist offsets. The Docker socket is highly privileged; in production, run the Agent on controlled hosts or restrict Docker API access with docker-socket-proxy.

## WebUI

Open:

```text
http://localhost:25091/ui/logs
```

Main pages:

- `/ui/logs`: query logs by service, instance, environment, level, trace ID, source ID, tokens, and metadata.
- `/ui/api-keys`: create, enable, disable, delete, and view API keys.
- `/ui/cleanup`: run manual log cleanup.

Deleting an API key does not delete its service or logs. A service is removed only when it has no API keys and no logs; manual log cleanup also triggers the empty-service cleanup pass.

## MQ Integration

The project provides:

- `MqLogReceiver`: receiver interface.
- `MqLogMessageDecoder`: decoder interface.
- `JsonMqLogMessageDecoder`: JSON payload decoder.
- `TestMqLogReceiver`: test receiver that uses the first enabled API key binding.

For a real MQ integration, implement a receiver for your broker and call the ingestion service with an authenticated or pre-resolved `LogSource`. Do not trust service ownership from MQ message payloads.

## Development

Run regular tests:

```bash
mvn test
```

Enable MongoDB integration tests:

```bash
export AIH_TEST_MONGODB_URI='mongodb://user:password@mongo-host:27017/aih_test?authSource=admin'
mvn test
```

Build the JAR:

```bash
mvn clean package
```

## Release

The GitHub Actions release workflow publishes only when all of these conditions are true:

1. `pom.xml` contains a final version such as `1.0.0`.
2. The version does not contain suffixes such as `SNAPSHOT`, `alpha`, `beta`, or `rc`.
3. The pushed tag name exactly matches the `pom.xml` version.

Example:

```bash
git tag 1.0.0
git push origin 1.0.0
```

Release outputs:

- Docker images: both the server and Docker Log Agent are published to GHCR.
- JAR files: both the server and Docker Log Agent are uploaded to the matching GitHub Release.

To also push the image to an extra private Docker registry, configure these GitHub Repository secrets:

| Secret | Description |
| --- | --- |
| `EXTRA_REGISTRY` | Registry host, for example `registry.example.com`. |
| `EXTRA_REGISTRY_NAMESPACE` | Registry namespace, for example `team` or `team/apps`. |
| `EXTRA_REGISTRY_USERNAME` | Registry username. |
| `EXTRA_REGISTRY_PASSWORD` | Registry password or access token. |

All four secrets must be configured to enable the extra push. Extra image names use this format:

```text
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written:<version>
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written:latest
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written-docker-agent:<version>
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written-docker-agent:latest
```

## Security

- Always set `AIH_API_KEY_ENCRYPTION_KEY`; startup fails if it is missing.
- Change `AIH_ADMIN_PASSWORD` and `AIH_DEFAULT_API_KEY` outside local development.
- Do not commit real MongoDB credentials, admin passwords, API keys, or encryption keys.
- Do not expose MongoDB publicly.
- Keep `/api/logs/**` reachable only by trusted services or an API gateway.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
