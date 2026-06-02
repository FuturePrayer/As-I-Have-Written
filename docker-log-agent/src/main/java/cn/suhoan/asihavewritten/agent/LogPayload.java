package cn.suhoan.asihavewritten.agent;

import java.time.Instant;
import java.util.Map;

public record LogPayload(
        Instant eventTime,
        String service,
        String environment,
        LogLevel level,
        String traceId,
        String spanId,
        String message,
        Map<String, Object> metadata) {
}
