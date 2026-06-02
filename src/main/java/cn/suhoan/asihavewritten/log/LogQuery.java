package cn.suhoan.asihavewritten.log;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LogQuery(
        Instant from,
        Instant to,
        String service,
        String instanceName,
        String environment,
        LogLevel level,
        String traceId,
        String sourceId,
        Map<String, String> metadataFilters,
        List<String> tokens,
        int page,
        int size) {
}
