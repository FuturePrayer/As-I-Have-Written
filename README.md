<p align="center">
  <img src="logo.png" alt="Peony" width="128" height="128">
</p>

<h1 align="center">As I Have Written</h1>

<p align="center">
<strong>As I Have Written（如我所书）是一个基于 Spring Boot 4 和 MongoDB 的轻量级日志接收与查询控制台。它支持通过 HTTP、批量 HTTP、NDJSON Streamable HTTP 以及可扩展 MQ 接收器写入日志，在应用侧完成分词，并提供 WebUI 用于按服务查询日志、管理 API Key、筛选自定义 metadata，以及切换中文和英文界面。</strong>
</p>

---

简体中文 | [English](README_en.md)

[![Release](https://img.shields.io/github/v/release/FuturePrayer/As-I-Have-Written?sort=semver)](https://github.com/FuturePrayer/As-I-Have-Written/releases)
[![Java 21](https://img.shields.io/badge/java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE.txt)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/FuturePrayer/As-I-Have-Written)

## 特性

- Spring Boot 4，默认启用虚拟线程。
- MongoDB 持久化，分词在应用侧完成。
- 支持单条 HTTP、批量 HTTP、NDJSON Streamable HTTP 写入。
- 提供可扩展 MQ 接收接口和 JSON 测试接收实现。
- WebUI 使用 Sa-Token 管理登录态，不依赖 Spring Security。
- API Key 绑定到 `Service + Instance`，日志归属以 API Key 绑定关系为准。
- API Key 使用 hash 做认证，并通过 AES-GCM 加密保存以支持 WebUI 重复查看。
- WebUI 支持服务选择、日志筛选、API Key 管理、自定义 metadata 精确筛选和中英文切换。
- 默认查询最近 15 分钟日志；一旦存在 Service，WebUI 查询必须选择 Service。

## 技术栈

- Java 21+
- Spring Boot 4
- Spring Web MVC
- Thymeleaf
- Sa-Token
- MongoDB
- Maven

## 快速开始

### Docker Compose（包含 MongoDB）

适合本地试用。Compose 会从当前源码构建应用镜像；MongoDB 只在 Compose 网络内暴露，数据保存在 Docker volume 中。

```bash
AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret' \
AIH_ADMIN_PASSWORD='replace-this-password' \
AIH_DEFAULT_API_KEY='replace-this-default-ingest-key' \
docker compose up -d
```

访问 WebUI：

```text
http://localhost:25091/ui/logs
```

### Docker Compose（外部 MongoDB）

当你已经有 MongoDB 实例时，使用不包含 MongoDB 的 Compose 文件。Compose 会从当前源码构建应用镜像：

```bash
MONGODB_URI='mongodb://user:password@mongo-host:27017/as_i_have_written?authSource=admin' \
AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret' \
AIH_ADMIN_PASSWORD='replace-this-password' \
AIH_DEFAULT_API_KEY='replace-this-default-ingest-key' \
docker compose -f docker-compose.external-mongodb.yml up -d
```

### 从源码运行

环境要求：

- JDK 21 或更高版本。
- Maven 3.9 或更高版本。
- MongoDB 6 或兼容版本。

启动：

```bash
export MONGODB_URI='mongodb://localhost:27017/as_i_have_written'
export AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret'
export AIH_ADMIN_USERNAME='admin'
export AIH_ADMIN_PASSWORD='replace-this-password'
export AIH_DEFAULT_API_KEY='replace-this-default-ingest-key'

mvn spring-boot:run
```

默认登录账号由 `AIH_ADMIN_USERNAME` 和 `AIH_ADMIN_PASSWORD` 控制。如果未配置，开发默认值为 `admin / admin123`；共享环境和生产环境必须修改。

## Docker 镜像

Release 工作流会将镜像发布到 GitHub Container Registry：

```text
ghcr.io/futureprayer/as-i-have-written:<version>
ghcr.io/futureprayer/as-i-have-written:latest
ghcr.io/futureprayer/as-i-have-written-docker-agent:<version>
ghcr.io/futureprayer/as-i-have-written-docker-agent:latest
```

中国大陆用户也可以使用加速镜像：

```text
swr.cn-east-3.myhuaweicloud.com/suhoan/as-i-have-written:latest
```

本地也可以直接构建镜像：

```bash
docker build -t as-i-have-written:local .
docker build -f docker-log-agent/Dockerfile -t as-i-have-written-docker-agent:local .
```

## 配置

应用读取 `src/main/resources/application.yml` 和环境变量。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `25091` | HTTP 服务端口。 |
| `SERVER_HTTP2_ENABLED` | `false` | 是否启用 HTTP/2；未启用 TLS 时使用 h2c，通常适合放在支持 h2c 的反向代理后。 |
| `MONGODB_URI` | `mongodb://localhost:27017/as_i_have_written` | MongoDB 连接地址。 |
| `AIH_ADMIN_USERNAME` | `admin` | WebUI 管理员用户名。 |
| `AIH_ADMIN_PASSWORD` | `admin123` | WebUI 管理员密码。非本地环境必须修改。 |
| `AIH_API_KEY_ENCRYPTION_KEY` | 无 | 必填。用于 AES-GCM 加密 API Key，缺失时启动失败。 |
| `AIH_INGEST_QUEUE_CAPACITY` | `10000` | 内存写入队列容量。 |
| `AIH_INGEST_BATCH_SIZE` | `500` | 后台写入批大小。 |
| `AIH_INGEST_FLUSH_INTERVAL` | `1s` | 后台写入刷新间隔。 |
| `AIH_DEFAULT_SERVICE_NAME` | `default` | 启动时创建的默认服务名。 |
| `AIH_DEFAULT_SERVICE_DISPLAY_NAME` | `Default` | 默认服务显示名。 |
| `AIH_DEFAULT_INSTANCE_NAME` | `default` | 启动时创建的默认实例名。 |
| `AIH_DEFAULT_API_KEY` | `dev-api-key` | 启动时创建的默认 API Key。非本地环境必须修改。 |
| `AIH_SERVICE_CLEANUP_FIXED_DELAY` | `10m` | 自动删除空 Service 的周期。 |
| `AIH_SERVICE_CLEANUP_INITIAL_DELAY` | `10m` | 首次执行空 Service 清理前的等待时间。 |

MongoDB 集合：

- `log_entries`：日志数据。
- `log_services`：WebUI 服务下拉选项。
- `log_sources`：`serviceName + instanceName` 对应的 API Key 绑定。

Spring Data MongoDB 会自动创建索引。MongoDB 只保存应用生成后的 `tokens` 并执行普通索引匹配，不负责全文分词。

## 性能调优

- 启用 `SERVER_HTTP2_ENABLED=true` 可以让支持 h2c 或 TLS HTTP/2 的客户端复用连接、减少高并发批量写入时的连接开销。Docker Log Agent 的 HTTP 客户端会优先使用 HTTP/2，并在服务端或代理不支持时回退到 HTTP/1.1。
- 写入侧优先使用 `/api/logs/batch` 或 NDJSON stream，不建议高频逐条调用 `/api/logs`。
- 服务端吞吐主要受 `AIH_INGEST_BATCH_SIZE`、`AIH_INGEST_FLUSH_INTERVAL`、`AIH_INGEST_QUEUE_CAPACITY` 和 MongoDB 写入能力影响。日志量较高时可以增大 batch 和 queue；如果更看重低延迟，可以缩短 flush interval。
- Docker Log Agent 默认 `AIH_AGENT_BATCH_SIZE=500`，和服务端默认写入批大小一致。多容器高吞吐场景可提高 `AIH_AGENT_QUEUE_CAPACITY`，但队列越大，Agent 进程内存占用也越高。
- MongoDB 建议放在同机房或低延迟网络中，并为 `log_entries` 所在数据库预留足够 IOPS。WebUI 查询性能依赖已有索引和查询范围，生产环境建议保留较短默认时间窗口。

## 写入 API

所有写入接口都需要：

```http
X-API-Key: <api-key>
```

`X-Log-Source` 已废弃，当前实现会忽略它。

请求体中的 `service` 字段仅用于校验和分词兼容；实际持久化的 `service` 和 `instanceName` 始终来自 API Key 绑定关系，客户端无法通过修改请求体伪造日志归属。

### 单条日志

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

### 批量日志

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

响应状态：

| 状态码 | 含义 |
| --- | --- |
| `202 Accepted` | 日志已进入内存队列。 |
| `400 Bad Request` | 请求体校验失败。 |
| `401 Unauthorized` | API Key 缺失、无效或已禁用。 |
| `429 Too Many Requests` | 写入队列已满。 |

## 其他 Spring Boot 项目接入

如果业务项目使用 Spring Boot 默认的 SLF4J + Logback，可以添加一个自定义 Logback appender，把日志异步批量发送到本项目：

```text
POST http://<aih-host>:25091/api/logs/batch
X-API-Key: <api-key>
```

请求体中的 `service`、`environment`、`level`、`message` 仍然必填。`service` 用于校验和分词兼容，最终持久化归属仍由 `X-API-Key` 对应的服务和实例绑定决定。

默认 Spring Boot Web 项目已经包含 Logback 和 Jackson。非 Web 项目至少需要保留日志和 JSON 能力：

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

将下面的类放到业务项目中，例如 `com.example.logging.AihLogbackAppender`。示例使用 Spring Boot 3.x 默认的 Jackson 2 包名；如果业务项目使用 Spring Boot 4 / Jackson 3，把 `ObjectMapper` 的 import 改为 `tools.jackson.databind.ObjectMapper`。

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

在业务项目的 `src/main/resources/logback-spring.xml` 中配置 appender：

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

启动业务项目前设置：

```bash
export AIH_LOG_ENDPOINT='http://localhost:25091/api/logs/batch'
export AIH_LOG_API_KEY='<api-key>'
export SPRING_APPLICATION_NAME='payment-service'
export SPRING_PROFILES_ACTIVE='prod'
```

示例 appender 不做持久化重试：远端不可用、HTTP 非 2xx 或队列已满时会丢弃日志并保留本地控制台输出。需要强可靠投递时，建议使用 MQ 或业务侧本地缓冲方案。

## Docker 容器日志 Agent

如果要采集 Docker 容器的 stdout/stderr 日志，可以使用内置 Docker Log Agent。它采集的是容器运行时日志，不是镜像本身的历史日志。Agent 通过 Docker socket 发现容器，并只采集显式开启的容器：

```text
aih.logs.enabled=true
```

Agent 使用每个容器的 `aih.logs.api-key-ref` 查找真实 API Key，再调用 `/api/logs/batch` 写入日志。不要把明文 API Key 写到 Docker label 中；label 会被 Docker API 和运维工具直接看到。

### Agent 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AIH_AGENT_ENDPOINT` | `http://localhost:25091/api/logs/batch` | 日志批量写入地址。 |
| `AIH_AGENT_API_KEY_FILE` | `/run/secrets/aih-agent-api-keys.properties` | API Key 引用文件，格式为 Java properties。 |
| `AIH_AGENT_API_KEYS` | 空 | 快速配置，格式 `ref1=key1;ref2=key2`；文件配置优先级更高。 |
| `AIH_AGENT_DEFAULT_ENVIRONMENT` | `local` | 容器未指定环境时使用。 |
| `AIH_AGENT_LOG_REGEX` | 空 | 全局日志解析正则，使用 Java named groups。 |
| `AIH_AGENT_LOG_REGEX_METADATA_GROUPS` | 空 | 需要写入 metadata 的额外正则分组名，逗号分隔。 |
| `AIH_AGENT_BATCH_SIZE` | `500` | 批量发送大小。 |
| `AIH_AGENT_QUEUE_CAPACITY` | `10000` | 出站队列容量。 |
| `AIH_AGENT_FLUSH_INTERVAL` | `1s` | 批量发送刷新间隔。 |
| `AIH_AGENT_REQUEST_TIMEOUT` | `5s` | 写入接口请求超时。 |
| `AIH_AGENT_DOCKER_HOST` | 空 | 可选 Docker Engine 地址；留空时使用 docker-java 默认发现规则。 |

容器 labels：

| Label | 说明 |
| --- | --- |
| `aih.logs.enabled=true` | 启用采集。 |
| `aih.logs.api-key-ref=<ref>` | 必填，从 Agent API Key 映射中查找真实 key。 |
| `aih.logs.service=<service>` | 可选，默认使用 Compose service，再退回容器名。 |
| `aih.logs.instance=<instance>` | 可选，默认容器名。 |
| `aih.logs.environment=<environment>` | 可选，默认 `AIH_AGENT_DEFAULT_ENVIRONMENT`。 |
| `aih.logs.regex=<regex>` | 可选，覆盖全局正则。 |
| `aih.logs.regex-metadata-groups=<names>` | 可选，覆盖全局 metadata 分组。 |

正则可使用这些内置分组：`time`、`level`、`traceId`、`spanId`、`message`。`time` 只解析 ISO-8601；`level` 只接受 `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR`。没有配置正则或匹配失败时，整行作为 `message`，stdout 映射为 `INFO`，stderr 映射为 `ERROR`。

### 使用 docker run

先创建 API Key 引用文件：

```properties
payment=replace-with-payment-api-key
order=replace-with-order-api-key
```

启动 Agent：

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

启动需要采集的业务容器：

```bash
docker run -d --name payment-api \
  --label aih.logs.enabled=true \
  --label aih.logs.api-key-ref=payment \
  --label aih.logs.service=payment \
  --label aih.logs.instance=payment-api-1 \
  --label aih.logs.environment=prod \
  example/payment-api:latest
```

### 使用 Docker Compose

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

Agent 会在 metadata 中写入 `containerId`、`containerName`、`image`、`stream`、`composeProject`、`composeService` 和安全筛选后的 Docker labels。启动后只跟随新日志，不补读历史日志，也不持久化 offset。Docker socket 权限很高，生产环境建议部署在受控主机，或通过 docker-socket-proxy 限制可访问的 Docker API。

## WebUI

访问：

```text
http://localhost:25091/ui/logs
```

主要页面：

- `/ui/logs`：查询日志，支持服务、实例、环境、级别、Trace ID、Source ID、tokens 和 metadata 过滤。
- `/ui/api-keys`：创建、启用、禁用、删除和查看 API Key。
- `/ui/cleanup`：执行手动日志清理。

删除 API Key 不会删除对应 Service 或日志。只有当某个 Service 同时没有 API Key 且没有日志时，后台清理或手动清理后的空 Service 清理才会删除它。

## MQ 集成

项目提供：

- `MqLogReceiver`：接收器接口。
- `MqLogMessageDecoder`：解码器接口。
- `JsonMqLogMessageDecoder`：JSON 消息解码器。
- `TestMqLogReceiver`：测试接收器，使用第一个启用的 API Key 绑定源。

真实 MQ 集成时，实现自己的 broker 接收器，并使用已认证或已解析的 `LogSource` 调用日志写入服务。不要信任 MQ 消息体中的服务归属。

## 开发

运行普通测试：

```bash
mvn test
```

启用 MongoDB 集成测试：

```bash
export AIH_TEST_MONGODB_URI='mongodb://user:password@mongo-host:27017/aih_test?authSource=admin'
mvn test
```

构建 JAR：

```bash
mvn clean package
```

## 发布

GitHub Actions release 工作流只会在满足以下条件时发布：

1. `pom.xml` 中的版本号是正式版本号，例如 `1.0.0`。
2. 版本号不包含 `SNAPSHOT`、`alpha`、`beta`、`rc` 等后缀。
3. 推送到远端的 tag 名称与 `pom.xml` 版本号完全一致。

示例：

```bash
git tag 1.0.0
git push origin 1.0.0
```

发布内容包括：

- Docker 镜像：服务端和 Docker Log Agent 都会发布到 GHCR。
- JAR 包：服务端和 Docker Log Agent 都会上传到同名 GitHub Release。

如果需要同时推送到额外的 Docker 私库，可以在 GitHub 仓库的 Repository secrets 中配置：

| Secret | 说明 |
| --- | --- |
| `EXTRA_REGISTRY` | 私库地址，例如 `registry.example.com`。 |
| `EXTRA_REGISTRY_NAMESPACE` | 私库命名空间，例如 `team` 或 `team/apps`。 |
| `EXTRA_REGISTRY_USERNAME` | 私库用户名。 |
| `EXTRA_REGISTRY_PASSWORD` | 私库密码或访问令牌。 |

四个 secret 必须同时配置才会启用额外推送。额外镜像地址格式为：

```text
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written:<version>
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written:latest
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written-docker-agent:<version>
<EXTRA_REGISTRY>/<EXTRA_REGISTRY_NAMESPACE>/as-i-have-written-docker-agent:latest
```

## 安全

- 必须配置 `AIH_API_KEY_ENCRYPTION_KEY`；缺失时应用启动失败。
- 非本地环境必须修改 `AIH_ADMIN_PASSWORD` 和 `AIH_DEFAULT_API_KEY`。
- 不要提交真实 MongoDB 凭据、管理员密码、API Key 或加密密钥。
- 不要把 MongoDB 暴露到公网。
- `/api/logs/**` 应只允许可信服务或 API 网关访问。

## License

本项目基于 Apache License 2.0 开源。详见 [LICENSE](LICENSE)。
