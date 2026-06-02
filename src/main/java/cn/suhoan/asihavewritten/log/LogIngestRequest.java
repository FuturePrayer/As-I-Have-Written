package cn.suhoan.asihavewritten.log;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LogIngestRequest(
        Instant eventTime,
        @NotBlank String service,
        @NotBlank String environment,
        @NotNull LogLevel level,
        String traceId,
        String spanId,
        @NotBlank String message,
        Map<String, Object> metadata) {
}
