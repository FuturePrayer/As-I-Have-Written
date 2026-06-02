# 中文说明

<img src="logo.png" alt="如我所书 logo" width="120">

English README: [README_en.md](README_en.md)

仓库地址：[FuturePrayer/As-I-Have-Written](https://github.com/FuturePrayer/As-I-Have-Written)

> 一个基于 Spring Boot 4 和 MongoDB 的轻量级日志接收与查询控制台。

As I Have Written 支持通过 HTTP、批量 HTTP、NDJSON Streamable HTTP 以及可扩展 MQ 接收器接收应用日志。系统使用 MongoDB 存储日志，在应用侧完成分词，并提供 WebUI 用于按服务查询日志、管理 API Key、筛选自定义维度以及切换中英文界面。

## 功能特性

- 基于 Spring Boot 4，默认启用虚拟线程。
- 使用 MongoDB 存储日志。
- 分词在应用中完成，MongoDB 只保存生成后的 `tokens` 并执行普通索引匹配。
- 支持单条 HTTP 写入、批量 HTTP 写入、NDJSON Streamable HTTP 写入。
- 提供可扩展 MQ 接收接口和 JSON 测试接收实现。
- WebUI 使用 Sa-Token 做登录态控制，不依赖 Spring Security。
- API Key 绑定到 `Service + Instance`。
- API Key 用 hash 做认证，用 AES-GCM 加密保存以支持 WebUI 重复查看。
- 日志归属以 API Key 绑定关系为准，不信任请求体中的 `service`。
- WebUI 中一旦存在 Service，查询时 Service 必选。
- 日志页默认查询最近 15 分钟。
- 自动发现日志 `metadata` 中的自定义维度，并支持精确筛选。
- 默认中文界面，支持中英文切换。
- 默认端口为 `25091`，可通过 `SERVER_PORT` 修改。

## 技术栈

- Java：Maven 编译目标为 Java 21；已使用 JDK 26 测试。
- 框架：Spring Boot 4.0.x。
- Web：Spring Web MVC、Thymeleaf。
- 认证：`sa-token-spring-boot4-starter`。
- 数据库：MongoDB。
- 构建：Maven。

## 环境要求

- JDK 21 或更高版本。本项目已使用 `D:\develop\jdk-26` 测试。
- Maven 3.9.x 或更高版本。本项目已使用 `D:\develop\apache-maven-3.9.6` 测试。
- MongoDB 6+ 或兼容版本。

## 快速开始

设置必要环境变量并启动：

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

访问：

```text
http://localhost:25091/ui/logs
```

登录用户名和密码由 `AIH_ADMIN_USERNAME`、`AIH_ADMIN_PASSWORD` 控制。如果未配置，默认是 `admin / admin123`；该默认值只适合本地开发。

## 配置说明

应用读取 `src/main/resources/application.yml` 和环境变量。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `25091` | HTTP 服务端口。 |
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
| `AIH_SERVICE_CLEANUP_FIXED_DELAY` | `10m` | 自动删除“没有 API Key 且没有日志”的 Service 的周期。 |
| `AIH_SERVICE_CLEANUP_INITIAL_DELAY` | `10m` | 首次执行空 Service 清理前的等待时间。 |

### MongoDB 说明

应用会使用以下集合：

- `log_entries`：日志数据。
- `log_services`：WebUI 服务下拉选项。
- `log_sources`：`serviceName + instanceName` 对应的 API Key 绑定。

由于启用了 `spring.data.mongodb.auto-index-creation=true`，MongoDB 索引会由 Spring Data MongoDB 自动创建。

分词逻辑始终在应用侧执行。MongoDB 不承担全文分词职责。

## 部署方式

### 方式一：使用 Maven 运行

适合本地开发或测试环境：

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

### 方式二：构建 JAR 后运行

构建：

```powershell
mvn clean package
```

运行：

```powershell
java -jar target\As-I-Have-Written-0.0.1-SNAPSHOT.jar
```

请确保 Java 进程能读取前文列出的环境变量。

### 方式三：Windows 服务或长期进程

用于长期运行时：

1. 使用 `mvn clean package` 构建 JAR。
2. 创建独立应用目录并复制 JAR。
3. 在服务级别配置环境变量，不要把密钥写进源码文件。
4. 使用 `java -jar` 启动。
5. 确保启动前 MongoDB 可连接。
6. 默认访问 `http://<host>:25091`，也可通过 `SERVER_PORT` 修改端口。

### 反向代理建议

应用可以放在反向代理后：

- 在反向代理层终止 TLS。
- 尽可能限制 `/ui/**` 只能由可信网络访问。
- `/api/logs/**` 建议只允许可信服务或 API 网关访问。
- 不要把 MongoDB 暴露到公网。

## WebUI 使用

访问：

```text
http://localhost:25091/ui/logs
```

未登录访问 `/ui/**` 时会跳转到：

```text
/ui/login?redirect=<原访问地址>
```

登录成功后，如果 redirect 是安全的站内相对路径，会回到原访问地址。

### 日志页

- 默认查询最近 15 分钟。
- 一旦系统存在 Service，查询时 Service 必选。
- 未传 Service 时自动选择按创建顺序排序的第一个 Service。
- 支持以下筛选：
  - 服务
  - 实例
  - 环境
  - 日志级别
  - Trace ID
  - Source ID
  - 应用生成的 tokens
  - 自动发现的 metadata 维度

### API Key 管理页

访问：

```text
/ui/api-keys
```

可执行：

- 创建服务和实例对应的 API Key。
- 从 datalist 复用已有 Service 名称。
- 启用或禁用 API Key。
- 删除 API Key，但不会删除 Service 或日志。
- 查看解密后的 API Key。

禁用 API Key 后，所有日志接收接口都会返回 HTTP `401`。
删除 API Key 后，该 key 会永久移除；对应 Service 会保留，因此可以继续为该 Service 重新生成 API Key。只有当某个 Service 同时没有 API Key 且没有日志时，后台才会自动删除它；手动清理日志后也会立即执行一次空 Service 清理。

### 语言切换

WebUI 默认中文。顶部导航的语言按钮可在中文和英文之间切换，并保留当前路径和查询参数。

## 日志写入 API

所有写入接口都需要：

```http
X-API-Key: <api-key>
```

`X-Log-Source` 已废弃，当前实现会忽略它。

重要归属规则：

- 请求体中仍保留 `service` 字段，用于校验和分词兼容。
- 实际持久化的 `service` 和 `instanceName` 始终来自 API Key 绑定关系。
- 客户端无法通过修改请求体中的 `service` 伪造日志归属。

### 日志请求体

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

必填字段：

- `service`
- `environment`
- `level`
- `message`

支持的日志级别：

- `TRACE`
- `DEBUG`
- `INFO`
- `WARN`
- `ERROR`

### 单条写入

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

响应示例：

```json
{"accepted":1}
```

### 批量写入

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

### 响应状态

| 状态码 | 含义 |
| --- | --- |
| `202 Accepted` | 日志已进入内存队列。 |
| `400 Bad Request` | 请求体校验失败。 |
| `401 Unauthorized` | API Key 缺失、无效或已禁用。 |
| `429 Too Many Requests` | 写入队列已满。 |

## MQ 集成

项目中包含：

- `MqLogReceiver`：接收器接口。
- `MqLogMessageDecoder`：解码器接口。
- `JsonMqLogMessageDecoder`：JSON 消息解码器。
- `TestMqLogReceiver`：测试接收器，使用第一个启用的 API Key 绑定源。

真实 MQ 集成时，实现自己的 broker 接收器，然后调用：

```java
ingestService.accept(new LogIngestCommand(source, IngestChannel.MQ_TEST, request));
```

请使用已认证或已解析的 `LogSource`，不要信任 MQ 消息体中的服务归属。

## 测试

不连接 MongoDB 时运行普通测试：

```powershell
mvn test
```

设置 `AIH_TEST_MONGODB_URI` 后会启用 MongoDB 集成测试：

```powershell
$env:AIH_TEST_MONGODB_URI='mongodb://user:password@mongo-host:27017/aih_test?authSource=admin'
mvn test
```

测试辅助类会自动注入 `aih.security.api-key-encryption-key`。

## 安全注意事项

- 必须配置 `AIH_API_KEY_ENCRYPTION_KEY`；缺失时应用启动失败。
- `AIH_API_KEY_ENCRYPTION_KEY` 应使用足够长的随机值。
- 非本地环境必须修改 `AIH_ADMIN_PASSWORD` 和 `AIH_DEFAULT_API_KEY`。
- 密钥应放在环境变量或密钥管理系统中，不要提交到仓库。
- API Key 认证使用 hash；WebUI 重复查看依赖加密存储。
- 禁用 API Key 后，该 key 会立即无法写入日志。
- MongoDB 应保持私有，只允许应用访问。

## 故障排查

### 启动失败：缺少 `AIH_API_KEY_ENCRYPTION_KEY`

设置：

```powershell
$env:AIH_API_KEY_ENCRYPTION_KEY='replace-with-a-long-random-secret'
```

然后重启应用。

### 端口被占用

设置其他端口：

```powershell
$env:SERVER_PORT='25092'
mvn spring-boot:run
```

Windows 下查看端口占用：

```powershell
netstat -ano | Select-String ':25091'
```

### 无法连接 MongoDB

检查：

- 主机和端口是否可达。
- 用户名和密码是否正确。
- `authSource` 是否正确。
- 数据库用户是否有创建集合和索引的权限。

### API 返回 401

检查：

- 是否传了 `X-API-Key`。
- 该 key 是否存在于 `/ui/api-keys`。
- 该 key 是否已启用。
- 使用的是明文 API Key，而不是 hash。

## License

本项目基于 Apache License 2.0 开源。详见 [LICENSE](LICENSE)。
