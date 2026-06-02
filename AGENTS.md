# AGENTS.md

This file gives coding agents and human contributors the project-specific context needed to work safely in this repository.

## Project Summary

As I Have Written is a Spring Boot 4 logging system backed by MongoDB. It accepts logs through HTTP, batch HTTP, NDJSON streamable HTTP, and a pluggable MQ receiver. The WebUI supports login, log querying, service selection, API key management, custom metadata filters, and Chinese/English switching.

## Current Architecture

- Main application: `src/main/java/cn/suhoan/asihavewritten/AsIHaveWrittenApplication.java`
- Auth and API key crypto: `src/main/java/cn/suhoan/asihavewritten/auth/`
- Configuration properties and MVC/Sa-Token setup: `src/main/java/cn/suhoan/asihavewritten/config/`
- Log domain, ingestion, querying, tokenization, service/source management: `src/main/java/cn/suhoan/asihavewritten/log/`
- Controllers and WebUI routing: `src/main/java/cn/suhoan/asihavewritten/web/`
- Thymeleaf templates: `src/main/resources/templates/`
- Static CSS: `src/main/resources/static/css/app.css`
- I18N resources:
  - `src/main/resources/messages_zh_CN.properties`
  - `src/main/resources/messages_en.properties`
- Main config: `src/main/resources/application.yml`
- License: `LICENSE` (Apache License 2.0)

## Important Behavioral Contracts

- Do not use Spring Security. This project intentionally uses Sa-Token.
- MongoDB is the persistence backend.
- Tokenization must happen in the application, not in MongoDB.
- Log ownership must come from the authenticated API key binding.
- Do not trust request-body `service` for persisted ownership.
- `X-API-Key` is the required ingestion credential.
- `X-Log-Source` is deprecated and should remain ignored if present.
- API keys are hash-checked for auth and AES-GCM encrypted for repeatable WebUI display.
- Deleting an API key must not delete its service or logs.
- Services are removed only when they have no API keys and no logs; manual log cleanup and the scheduled unused-service cleanup perform this removal.
- `AIH_API_KEY_ENCRYPTION_KEY` is required. Do not add an insecure production default.
- Once services exist, WebUI log queries must require a service.
- The WebUI default query range is the last 15 minutes.
- WebUI text should use message keys so Chinese and English both work.
- Default server port is `25091`, unless `SERVER_PORT` is set.

## Development Environment

The known local toolchain is:

```powershell
$env:JAVA_HOME='D:\develop\jdk-26'
$env:PATH='D:\develop\jdk-26\bin;D:\develop\apache-maven-3.9.6\bin;' + $env:PATH
```

The Maven build compiles with Java release 21 and has been tested using JDK 26.

## Common Commands

Run tests without MongoDB integration:

```powershell
mvn test
```

Run MongoDB integration tests:

```powershell
$env:AIH_TEST_MONGODB_URI='mongodb://user:password@host:27017/aih_test?authSource=admin'
mvn test
```

Run the app locally:

```powershell
$env:MONGODB_URI='mongodb://localhost:27017/as_i_have_written'
$env:AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret'
$env:AIH_ADMIN_USERNAME='admin'
$env:AIH_ADMIN_PASSWORD='replace-this-password'
$env:AIH_DEFAULT_API_KEY='replace-this-default-ingest-key'
mvn spring-boot:run
```

Build a JAR:

```powershell
mvn clean package
```

## Testing Notes

- Some Spring Boot tests are intentionally gated by `AIH_TEST_MONGODB_URI`.
- Without that environment variable, MongoDB-dependent tests are skipped.
- `src/test/java/cn/suhoan/asihavewritten/TestMongoProperties.java` injects a test encryption key for Mongo-backed tests.
- If an external MongoDB test database has stale data, prefer using a fresh database name instead of deleting user data.

## Security Rules

- Never commit real MongoDB credentials, admin passwords, API keys, or encryption keys.
- Keep project license metadata aligned across `LICENSE`, `README.md`, `README_zh-CN.md`, and `pom.xml`.
- Use placeholders in docs and code examples.
- Do not log plaintext API keys.
- Do not expose API key hashes in WebUI or API responses.
- Preserve safe redirect checks for login and locale switching.
- Keep `/api/logs/**` outside WebUI login interception.
- Treat `metadata` keys as user input. Preserve key validation before building `metadata.<key>` MongoDB criteria.

## Frontend and I18N Rules

- The UI is a functional operations console, not a marketing page.
- Keep controls compact and scannable.
- Do not add landing-page style hero sections.
- Add any new UI text to both message files.
- Chinese is the default locale.
- Language switching should preserve the current relative path and query string.

## Data Model Notes

- `LogService` controls service ordering and enabled state.
- `LogSource` stores API key bindings for `serviceName + instanceName`.
- `LogEntry` stores persisted logs and includes `instanceName`, `metadata`, `tokens`, and `ingestChannel`.
- Unique service/instance API key bindings are enforced on `log_sources`.
- Default source initialization is intended to be idempotent and compatible with old default-key records.
- Unused service cleanup is controlled by `AIH_SERVICE_CLEANUP_FIXED_DELAY` and `AIH_SERVICE_CLEANUP_INITIAL_DELAY`.

## Code Style Notes

- Follow the existing package boundaries.
- Keep changes scoped.
- Prefer Spring Data repository methods and `MongoTemplate` criteria over ad hoc string manipulation.
- Keep code comments rare and useful.
- Prefer focused tests for behavior changes.

## Known Operational Defaults

- WebUI URL: `http://localhost:25091/ui/logs`
- Default admin username: `admin`
- Default admin password: `admin123`
- Default service: `default`
- Default instance: `default`
- Default API key: `dev-api-key`

These defaults are for local development only. Production or shared deployments must override them.
