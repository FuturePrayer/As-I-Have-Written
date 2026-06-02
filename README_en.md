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
```

Users in mainland China can also use the accelerated mirror:

```text
swr.cn-east-3.myhuaweicloud.com/suhoan/as-i-have-written:latest
```

You can also build an image locally:

```bash
docker build -t as-i-have-written:local .
```

## Configuration

The application reads `src/main/resources/application.yml` and environment variables.

| Environment variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `25091` | HTTP server port. |
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

- Docker image: published to GHCR.
- JAR file: uploaded to the matching GitHub Release.

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
```

## Security

- Always set `AIH_API_KEY_ENCRYPTION_KEY`; startup fails if it is missing.
- Change `AIH_ADMIN_PASSWORD` and `AIH_DEFAULT_API_KEY` outside local development.
- Do not commit real MongoDB credentials, admin passwords, API keys, or encryption keys.
- Do not expose MongoDB publicly.
- Keep `/api/logs/**` reachable only by trusted services or an API gateway.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
