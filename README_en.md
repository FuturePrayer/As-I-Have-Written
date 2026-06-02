# As I Have Written

<img src="logo.png" alt="As I Have Written logo" width="120">

> A lightweight Spring Boot 4 log ingestion and query console backed by MongoDB.

As I Have Written is a small log platform for receiving application logs over HTTP, batch HTTP, NDJSON streamable HTTP, and a pluggable MQ receiver. It stores logs in MongoDB, performs tokenization in the application, and provides a WebUI for service-scoped querying, API key management, custom metadata filtering, and Chinese/English UI switching.

中文文档: [README.md](README.md)

Repository: [FuturePrayer/As-I-Have-Written](https://github.com/FuturePrayer/As-I-Have-Written)

## Features

- Spring Boot 4, Java 21+ bytecode, and virtual threads enabled by default.
- MongoDB storage with application-side tokenization.
- HTTP single-log ingestion, HTTP batch ingestion, and NDJSON streamable HTTP ingestion.
- Pluggable MQ receiver interface with a test JSON receiver implementation.
- WebUI protected by Sa-Token, without Spring Security.
- Service and instance scoped API keys.
- API keys are hashed for authentication and encrypted with AES-GCM for repeatable display in WebUI.
- Log ownership is resolved from the API key binding. Request-body `service` is not trusted for persisted ownership.
- Required service selector in WebUI once services exist.
- Default WebUI log query range is the last 15 minutes.
- Auto-discovered custom metadata dimensions with exact filtering.
- Chinese UI by default, with an English/Chinese switch.
- Default server port is `25091`, configurable with `SERVER_PORT`.

## Tech Stack

- Java: builds with Maven using `maven.compiler.release=21`; tested with JDK 26.
- Framework: Spring Boot 4.0.x.
- Web: Spring Web MVC and Thymeleaf.
- Auth: `sa-token-spring-boot4-starter`.
- Database: MongoDB.
- Build: Maven.

## Requirements

- JDK 21 or newer. The project has been tested with `D:\develop\jdk-26`.
- Maven 3.9.x or newer. The project has been tested with `D:\develop\apache-maven-3.9.6`.
- MongoDB 6+ or a compatible MongoDB deployment.

## Quick Start

Set the required environment variables and start the application:

```powershell
$env:JAVA_HOME='D:\develop\jdk-26'
$env:PATH='D:\develop\jdk-26\bin;D:\develop\apache-maven-3.9.6\bin;' + $env:PATH

$env:MONGODB_URI='mongodb://localhost:27017/as_i_have_written'
$env:AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret'
$env:AIH_ADMIN_USERNAME='admin'
$env:AIH_ADMIN_PASSWORD='replace-this-password'
$env:AIH_DEFAULT_API_KEY='replace-this-default-ingest-key'

mvn spring-boot:run
```

Open:

```text
http://localhost:25091/ui/logs
```

The default login user is controlled by `AIH_ADMIN_USERNAME` and `AIH_ADMIN_PASSWORD`. If they are not set, the application defaults to `admin / admin123`; this default is only suitable for local development.

## Configuration

The application reads configuration from `src/main/resources/application.yml` and environment variables.

| Environment variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `25091` | HTTP server port. |
| `MONGODB_URI` | `mongodb://localhost:27017/as_i_have_written` | MongoDB connection URI. |
| `AIH_ADMIN_USERNAME` | `admin` | WebUI administrator username. |
| `AIH_ADMIN_PASSWORD` | `admin123` | WebUI administrator password. Change this in every non-local environment. |
| `AIH_API_KEY_ENCRYPTION_KEY` | none | Required. AES-GCM key material used to encrypt API keys for repeatable display. Startup fails if missing. |
| `AIH_INGEST_QUEUE_CAPACITY` | `10000` | In-memory ingestion queue capacity. |
| `AIH_INGEST_BATCH_SIZE` | `500` | Writer batch size. |
| `AIH_INGEST_FLUSH_INTERVAL` | `1s` | Writer flush interval. |
| `AIH_DEFAULT_SERVICE_NAME` | `default` | Default service created on startup. |
| `AIH_DEFAULT_SERVICE_DISPLAY_NAME` | `Default` | Display name for the default service. |
| `AIH_DEFAULT_INSTANCE_NAME` | `default` | Default instance created on startup. |
| `AIH_DEFAULT_API_KEY` | `dev-api-key` | Default API key created on startup. Change this outside local development. |
| `AIH_SERVICE_CLEANUP_FIXED_DELAY` | `10m` | Interval for automatically removing services that have no API keys and no logs. |
| `AIH_SERVICE_CLEANUP_INITIAL_DELAY` | `10m` | Initial delay before the first unused-service cleanup run. |

### MongoDB Notes

The application stores:

- `log_entries`: persisted logs.
- `log_services`: service definitions used by the WebUI selector.
- `log_sources`: API key bindings for `serviceName + instanceName`.

MongoDB indexes are created automatically through Spring Data MongoDB because `spring.data.mongodb.auto-index-creation=true` is enabled.

Tokenization is always done in the application. MongoDB stores the generated `tokens` array and performs ordinary indexed matching; it does not perform full-text tokenization.

## Deployment

### Option 1: Run with Maven

This is the simplest local or development deployment:

```powershell
$env:JAVA_HOME='D:\develop\jdk-26'
$env:PATH='D:\develop\jdk-26\bin;D:\develop\apache-maven-3.9.6\bin;' + $env:PATH
$env:MONGODB_URI='mongodb://user:password@mongo-host:27017/as_i_have_written?authSource=admin'
$env:AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret'
$env:AIH_ADMIN_USERNAME='admin'
$env:AIH_ADMIN_PASSWORD='replace-this-password'
$env:AIH_DEFAULT_API_KEY='replace-this-default-ingest-key'
mvn spring-boot:run
```

### Option 2: Build and Run a JAR

Build:

```powershell
mvn clean package
```

Run:

```powershell
java -jar target\As-I-Have-Written-0.0.1-SNAPSHOT.jar
```

Make sure the same environment variables listed above are available to the Java process.

### Option 3: Windows Service or Scheduled Process

For a long-running Windows deployment:

1. Build the JAR with `mvn clean package`.
2. Create a dedicated directory for the application and copy the JAR there.
3. Configure environment variables at the service level, not in source files.
4. Start the process with `java -jar`.
5. Ensure MongoDB is reachable before startup.
6. Route traffic to `http://<host>:25091` or set `SERVER_PORT` to the desired internal port.

### Reverse Proxy

The application can run behind a reverse proxy. Recommended baseline:

- Terminate TLS at the proxy.
- Restrict access to `/ui/**` to trusted networks when possible.
- Keep `/api/logs/**` reachable only by trusted services or through an API gateway.
- Do not expose MongoDB publicly.

## WebUI Usage

Open:

```text
http://localhost:25091/ui/logs
```

When not logged in, `/ui/**` redirects to:

```text
/ui/login?redirect=<original-path>
```

After login, the user is returned to the original path if the redirect is a safe relative URL.

### Logs Page

- Defaults to the last 15 minutes.
- Requires a service once at least one service exists.
- Selects the first service by creation order when no service is provided.
- Supports filters for:
  - service
  - instance
  - environment
  - level
  - trace ID
  - source ID
  - application-generated tokens
  - auto-discovered metadata keys

### API Keys Page

Open:

```text
/ui/api-keys
```

You can:

- Create a service and instance API key.
- Reuse an existing service name from the datalist.
- Enable or disable API keys.
- Delete API keys without deleting their services or logs.
- View encrypted-at-rest API keys after decryption in the WebUI.

Disabled API keys are rejected by all ingestion APIs with HTTP `401`.
Deleted API keys are removed permanently. The service remains available so a new API key can be created for it. Services are automatically removed only after they have no API keys and no logs; manual log cleanup triggers the same unused-service cleanup immediately.

### Language Switch

The WebUI defaults to Chinese. Use the language button in the top navigation to switch between Chinese and English. The current path and query string are preserved.

## Ingestion API

All ingestion endpoints require:

```http
X-API-Key: <api-key>
```

`X-Log-Source` is deprecated and ignored by the current implementation.

Important ownership rule:

- The request body still contains a `service` field for validation/tokenization compatibility.
- Persisted `service` and `instanceName` are always resolved from the API key binding.
- A client cannot spoof ownership by changing request-body `service`.

### Log Payload

```json
{
  "eventTime": "2026-06-02T01:00:00Z",
  "service": "ignored-for-ownership",
  "environment": "prod",
  "level": "ERROR",
  "traceId": "trace-123",
  "spanId": "span-456",
  "message": "payment failed",
  "metadata": {
    "threadName": "worker-1",
    "region": "ap-east-1"
  }
}
```

Required fields:

- `service`
- `environment`
- `level`
- `message`

Supported levels:

- `TRACE`
- `DEBUG`
- `INFO`
- `WARN`
- `ERROR`

### Single Log

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:25091/api/logs' `
  -Headers @{ 'X-API-Key' = '<api-key>' } `
  -ContentType 'application/json' `
  -Body '{
    "service":"client-value",
    "environment":"prod",
    "level":"INFO",
    "message":"single log",
    "metadata":{"threadName":"main"}
  }'
```

Expected response:

```json
{"accepted":1}
```

### Batch Logs

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:25091/api/logs/batch' `
  -Headers @{ 'X-API-Key' = '<api-key>' } `
  -ContentType 'application/json' `
  -Body '[
    {"service":"client-value","environment":"prod","level":"INFO","message":"one"},
    {"service":"client-value","environment":"prod","level":"WARN","message":"two"}
  ]'
```

### NDJSON Streamable HTTP

```powershell
$body = @'
{"service":"client-value","environment":"prod","level":"INFO","message":"one"}
{"service":"client-value","environment":"prod","level":"WARN","message":"two"}
'@

Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:25091/api/logs/stream' `
  -Headers @{ 'X-API-Key' = '<api-key>' } `
  -ContentType 'application/x-ndjson' `
  -Body $body
```

### Responses

| Status | Meaning |
| --- | --- |
| `202 Accepted` | Logs were accepted into the in-memory queue. |
| `400 Bad Request` | Payload validation failed. |
| `401 Unauthorized` | API key is missing, invalid, or disabled. |
| `429 Too Many Requests` | Ingestion queue is full. |

## MQ Integration

The project contains:

- `MqLogReceiver`: receiver interface.
- `MqLogMessageDecoder`: decoder interface.
- `JsonMqLogMessageDecoder`: JSON payload decoder.
- `TestMqLogReceiver`: test receiver that uses the first enabled API key binding.

For a real MQ integration, implement a receiver for your broker and call:

```java
ingestService.accept(new LogIngestCommand(source, IngestChannel.MQ_TEST, request));
```

Use an authenticated or pre-resolved `LogSource`; do not trust service ownership from MQ message payloads.

## Tests

Run unit tests without MongoDB integration:

```powershell
mvn test
```

MongoDB-dependent tests are enabled only when `AIH_TEST_MONGODB_URI` is set:

```powershell
$env:AIH_TEST_MONGODB_URI='mongodb://user:password@mongo-host:27017/aih_test?authSource=admin'
mvn test
```

The test helper injects `aih.security.api-key-encryption-key` automatically for tests.

## Security Notes

- Always set `AIH_API_KEY_ENCRYPTION_KEY`; startup fails if it is missing.
- Use a long random value for `AIH_API_KEY_ENCRYPTION_KEY`.
- Change `AIH_ADMIN_PASSWORD` and `AIH_DEFAULT_API_KEY` before any shared or production deployment.
- Store secrets in environment variables or a secret manager. Do not commit them.
- API key authentication uses hashes; repeatable key display uses encrypted storage.
- Disabling an API key immediately prevents ingestion through that key.
- Keep MongoDB private and reachable only by the application.

## Troubleshooting

### Startup Fails Because `AIH_API_KEY_ENCRYPTION_KEY` Is Missing

Set:

```powershell
$env:AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret'
```

Then restart the application.

### Port Is Already in Use

Set another port:

```powershell
$env:SERVER_PORT='25092'
mvn spring-boot:run
```

On Windows, check port listeners:

```powershell
netstat -ano | Select-String ':25091'
```

### Cannot Connect to MongoDB

Verify:

- Host and port are reachable.
- Username and password are correct.
- `authSource` is correct.
- The database user can create collections and indexes.

### API Returns 401

Check:

- `X-API-Key` header is present.
- The key exists in `/ui/api-keys`.
- The key is enabled.
- You are using the plain API key, not the hash.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
